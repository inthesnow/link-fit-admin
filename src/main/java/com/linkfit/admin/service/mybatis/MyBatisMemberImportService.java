package com.linkfit.admin.service.mybatis;

import com.linkfit.admin.domain.Locker;
import com.linkfit.admin.domain.Member;
import com.linkfit.admin.domain.Membership;
import com.linkfit.admin.mapper.LockerMapper;
import com.linkfit.admin.mapper.MemberMapper;
import com.linkfit.admin.service.MemberImportService;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddressList;
import org.apache.poi.xssf.usermodel.XSSFDataValidationHelper;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 타사 CRM 회원 정보를 우리 템플릿 엑셀로 받아 일괄 등록한다.
 * 컬럼 순서(A~U)는 {@link #HEADERS}와 반드시 일치해야 한다 — 헤더 텍스트가 아니라
 * 열 위치로 파싱하기 때문(사용자가 헤더 문구를 임의로 바꿔도 안전하게 동작하도록).
 */
@Service
public class MyBatisMemberImportService implements MemberImportService {

    private static final Logger log = LoggerFactory.getLogger(MyBatisMemberImportService.class);

    private static final String[] HEADERS = {
            "이름", "연락처", "성별", "생년월일", "이메일",
            "회원권구분", "상품명", "이용시작일", "이용종료일", "PT횟수",
            "결제금액", "할인금액", "실결제금액", "결제수단", "담당트레이너",
            "락커번호", "락커시작일", "락커종료일", "운동복시작일", "운동복종료일", "메모"
    };
    private static final String[] EXAMPLE_ROW = {
            "홍길동", "010-1234-5678", "남자", "1990-01-01", "",
            "회원권", "3개월 회원권", "2026-08-01", "2026-11-01", "",
            "300000", "0", "300000", "카드", "김트레이너",
            "12", "2026-08-01", "2026-11-01", "", "", "(예시 행 - 실제 데이터 입력 전 이 행은 삭제하세요)"
    };
    private static final String[] GENDER_OPTIONS = {"남자", "여자"};
    private static final String[] TYPE_OPTIONS = {"회원권", "PT", "그룹"};
    private static final Map<String, String> TYPE_MAP = Map.of(
            "회원권", "MEMBERSHIP",
            "PT", "PT",
            "그룹", "GROUP"
    );
    // PT는 기간이 아니라 횟수로 관리 — MyBatisMemberService의 무기한 처리와 동일한 값
    // (membership.end_date가 NOT NULL이라 값 자체는 필요하지만 실제 만료 판단엔 안 쓰임)
    private static final int PT_UNLIMITED_DURATION_DAYS = 3650;
    private static final DateTimeFormatter[] DATE_FORMATS = {
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),
            DateTimeFormatter.ofPattern("yyyy.MM.dd"),
            DateTimeFormatter.ofPattern("yyyy/MM/dd"),
            DateTimeFormatter.ofPattern("yyyyMMdd")
    };

    private final MemberMapper memberMapper;
    private final LockerMapper lockerMapper;

    public MyBatisMemberImportService(MemberMapper memberMapper, LockerMapper lockerMapper) {
        this.memberMapper = memberMapper;
        this.lockerMapper = lockerMapper;
    }

    @Override
    public byte[] generateTemplate() {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            XSSFSheet sheet = wb.createSheet("회원목록");

            CellStyle headerStyle = wb.createCellStyle();
            Font headerFont = wb.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            CellStyle exampleStyle = wb.createCellStyle();
            Font exampleFont = wb.createFont();
            exampleFont.setItalic(true);
            exampleFont.setColor(IndexedColors.GREY_50_PERCENT.getIndex());
            exampleStyle.setFont(exampleFont);

            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < HEADERS.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(HEADERS[i]);
                cell.setCellStyle(headerStyle);
                sheet.setColumnWidth(i, 4000);
            }

            Row exampleRow = sheet.createRow(1);
            for (int i = 0; i < EXAMPLE_ROW.length; i++) {
                Cell cell = exampleRow.createCell(i);
                cell.setCellValue(EXAMPLE_ROW[i]);
                cell.setCellStyle(exampleStyle);
            }

            XSSFDataValidationHelper validationHelper = new XSSFDataValidationHelper(sheet);
            addDropdown(sheet, validationHelper, GENDER_OPTIONS, 2, 2, 1000);   // C열: 성별
            addDropdown(sheet, validationHelper, TYPE_OPTIONS, 5, 5, 1000);     // F열: 회원권구분

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            wb.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("템플릿 생성에 실패했습니다.", e);
        }
    }

    private void addDropdown(XSSFSheet sheet, XSSFDataValidationHelper helper, String[] options,
                              int firstCol, int lastCol, int lastRow) {
        DataValidationConstraint constraint = helper.createExplicitListConstraint(options);
        CellRangeAddressList range = new CellRangeAddressList(1, lastRow, firstCol, lastCol);
        DataValidation validation = helper.createValidation(constraint, range);
        validation.setShowErrorBox(true);
        sheet.addValidationData(validation);
    }

    @Override
    @Transactional
    public List<Map<String, Object>> importExcel(InputStream excel, Long gymId) {
        List<Map<String, Object>> results = new ArrayList<>();
        DataFormatter formatter = new DataFormatter();

        try (Workbook wb = WorkbookFactory.create(excel)) {
            Sheet sheet = wb.getSheetAt(0);
            int lastRow = sheet.getLastRowNum();

            // 0행=헤더, 1행=예시 — 2행부터 실제 데이터
            for (int r = 2; r <= lastRow; r++) {
                Row row = sheet.getRow(r);
                if (row == null || isRowBlank(row, formatter)) continue;

                int excelRowNumber = r + 1; // 사용자에게 보여줄 때는 엑셀 화면상 행 번호(1-base)
                Map<String, Object> result = new LinkedHashMap<>();
                List<String> messages = new ArrayList<>();
                result.put("row", excelRowNumber);

                String name = str(row, 0, formatter);
                String phone = normalizePhone(str(row, 1, formatter));
                result.put("name", name);
                result.put("phone", phone);

                if (name == null || name.isBlank() || phone == null || phone.isBlank()) {
                    result.put("success", false);
                    messages.add("이름과 연락처는 필수입니다.");
                    result.put("messages", messages);
                    results.add(result);
                    continue;
                }

                // 이름+연락처가 기존 앱 회원과 일치하면(어느 지점 소속이든) 새 회원을 만들지 않고
                // 그 계정을 이 지점에 연결한다("앱 활성화") — 이미 이 지점에 연결돼 있으면 진짜 중복.
                Optional<String> existingId = memberMapper.findIdByNameAndPhone(name, phone);
                boolean isExistingAppUser = existingId.isPresent();
                if (isExistingAppUser && memberMapper.existsInGym(existingId.get(), gymId)) {
                    result.put("success", false);
                    messages.add("이미 이 지점에 등록된 회원입니다.");
                    result.put("messages", messages);
                    results.add(result);
                    continue;
                }

                // 회원 생성/연결 자체가 실패하면 이 행은 완전히 실패(success=false) — 재업로드 가능.
                // 회원은 연결됐는데 이용권/락커/운동복 중 일부만 실패한 경우는 success=true로 두고
                // messages에 실패 사유를 남긴다 — 재업로드하면 "이미 등록됨" 처리되어 혼동을 줄 수 있고,
                // 상세정보 화면에서 수동으로 이어서 등록하면 되기 때문.
                try {
                    String memberId;
                    if (isExistingAppUser) {
                        memberId = existingId.get();
                        memberMapper.insertUserGym(memberId, gymId);
                        messages.add("기존 앱 회원을 지점에 연결했습니다 (앱 활성화)");
                    } else {
                        Member member = new Member();
                        member.setName(name);
                        member.setPhone(phone);
                        member.setGender(str(row, 2, formatter));
                        member.setBirthDate(parseDate(row, 3, formatter));
                        String email = str(row, 4, formatter);
                        member.setEmail(email == null || email.isBlank() ? null : email);
                        member.setId(UUID.randomUUID().toString());
                        member.setMemberType(null);
                        member.setTier("BASIC");
                        // 엑셀엔 가입일 컬럼이 없다 — created_at(=이관 작업 실행 시각)을 가입일처럼
                        // 보여주면 "오늘 가입한 회원"으로 오해하게 되므로, 화면엔 공란으로 표시한다.
                        member.setJoinDateUnknown(true);

                        memberMapper.insertUser(member);
                        memberMapper.insertProfile(member);
                        memberMapper.insertUserGym(member.getId(), gymId);
                        messages.add("회원 등록 완료");
                        memberId = member.getId();
                    }
                    result.put("success", true);

                    try {
                        importMembership(row, formatter, memberId, gymId, messages);
                    } catch (Exception e) {
                        log.error("[MemberImport] {}행 이용권 등록 실패", excelRowNumber, e);
                        messages.add("이용권 등록 중 오류가 발생해 건너뛰었습니다: " + e.getMessage());
                    }
                    try {
                        importLocker(row, formatter, memberId, gymId, messages);
                    } catch (Exception e) {
                        log.error("[MemberImport] {}행 락커 배정 실패", excelRowNumber, e);
                        messages.add("락커 배정 중 오류가 발생해 건너뛰었습니다: " + e.getMessage());
                    }
                    try {
                        importUniform(row, formatter, memberId, gymId, messages);
                    } catch (Exception e) {
                        log.error("[MemberImport] {}행 운동복 등록 실패", excelRowNumber, e);
                        messages.add("운동복 등록 중 오류가 발생해 건너뛰었습니다: " + e.getMessage());
                    }
                } catch (Exception e) {
                    log.error("[MemberImport] {}행 회원 등록/연결 실패", excelRowNumber, e);
                    messages.add("회원 등록/연결 중 오류: " + e.getMessage());
                    result.put("success", false);
                }

                result.put("messages", messages);
                results.add(result);
            }
        } catch (IOException e) {
            throw new IllegalArgumentException("엑셀 파일을 읽을 수 없습니다. 템플릿 형식을 확인해주세요.", e);
        }

        return results;
    }

    private void importMembership(Row row, DataFormatter formatter, String memberId, Long gymId, List<String> messages) {
        String typeLabel = str(row, 5, formatter);
        if (typeLabel == null || typeLabel.isBlank()) return; // 이용권 정보 없음(선택)

        String type = TYPE_MAP.get(typeLabel.trim());
        if (type == null) {
            messages.add("회원권구분 값('" + typeLabel + "')을 인식할 수 없어 이용권은 등록하지 않았습니다. (회원권/PT/그룹 중 하나여야 함)");
            return;
        }
        LocalDate start = parseDate(row, 7, formatter);
        if (start == null) {
            messages.add("이용시작일이 없어 이용권을 등록하지 않았습니다.");
            return;
        }
        LocalDate end = parseDate(row, 8, formatter);
        boolean alreadyExpired = false;
        if (end == null) {
            if ("PT".equals(type)) {
                end = start.plusDays(PT_UNLIMITED_DURATION_DAYS); // PT는 기간이 아니라 횟수로 관리
            } else {
                // 이용종료일이 비어있는 경우는 타사 CRM에서 이미 만료된 회원을 그렇게 표기하던 관행 —
                // 임의의 종료일을 추정하지 않고, 오늘 이전 날짜로 등록해 만료 상태로 분류한다.
                end = LocalDate.now().minusDays(1);
                alreadyExpired = true;
            }
        }

        Membership m = new Membership();
        m.setMemberId(memberId);
        m.setType(type);
        m.setProductName(blankToNull(str(row, 6, formatter)));
        m.setStartDate(start);
        m.setEndDate(end);
        m.setPrice(intVal(row, 10, formatter, 0));
        m.setDiscountAmount(intVal(row, 11, formatter, 0));
        m.setPaidAmount(intVal(row, 12, formatter, 0));
        m.setPaymentMethod(blankToNull(str(row, 13, formatter)));
        m.setRegType("NEW");
        // 이용종료일 미기재로 만든 행은 status를 'EXPIRED_UNKNOWN'으로 표시해 다른 곳의
        // "status IS NULL"(=정상/유효 이용권) 조건에서 자동으로 빠지게 한다 — 별도 예외 처리
        // 없이도 유효회원 집계/만료일 표시 등에서 "이용권 없음(=만료)"과 동일하게 취급됨.
        m.setStatus(alreadyExpired ? "EXPIRED_UNKNOWN" : null);
        m.setMemo("타사 CRM 이관 등록");
        if ("PT".equals(type)) {
            Integer sessionCount = intValOrNull(row, 9, formatter);
            m.setSessionCount(sessionCount);
        }
        memberMapper.insertMembership(m, gymId);
        if ("PT".equals(type) && m.getSessionCount() != null && m.getSessionCount() != 0) {
            memberMapper.adjustPtSessions(memberId, m.getSessionCount(), gymId);
        }
        messages.add(alreadyExpired
                ? "이용권(" + typeLabel + ") 등록 완료 (이용종료일 미기재 — 이미 만료된 회원으로 분류)"
                : "이용권(" + typeLabel + ") 등록 완료");

        String trainerName = str(row, 14, formatter);
        if (trainerName != null && !trainerName.isBlank()) {
            messages.add("담당 트레이너('" + trainerName + "')는 자동 매칭되지 않습니다 — 상세정보에서 직접 지정해주세요.");
        }
    }

    private void importLocker(Row row, DataFormatter formatter, String memberId, Long gymId, List<String> messages) {
        Integer lockerNumber = intValOrNull(row, 15, formatter);
        if (lockerNumber == null) return; // 락커 정보 없음(선택)

        LocalDate start = parseDate(row, 16, formatter);
        LocalDate end = parseDate(row, 17, formatter);
        if (start == null || end == null) {
            messages.add("락커 이용기간(시작/종료일)이 없어 락커는 배정하지 않았습니다.");
            return;
        }

        List<Locker> candidates = lockerMapper.findAvailableByGymAndNumber(gymId, lockerNumber);
        Locker target = candidates.stream().filter(l -> l.getMembershipId() == null).findFirst().orElse(null);
        if (target == null) {
            if (candidates.isEmpty()) {
                messages.add(lockerNumber + "번 락커를 찾을 수 없어 배정하지 않았습니다 — 라커 관리 화면에서 먼저 구역/번호를 생성해주세요.");
            } else {
                messages.add(lockerNumber + "번 락커가 이미 배정되어 있어 배정하지 않았습니다.");
            }
            return;
        }

        Membership m = new Membership();
        m.setMemberId(memberId);
        m.setType("LOCKER");
        m.setLockerId(target.getId());
        m.setStartDate(start);
        m.setEndDate(end);
        m.setRegType("NEW");
        m.setMemo("타사 CRM 이관 등록");
        memberMapper.insertMembership(m, gymId);
        messages.add(lockerNumber + "번 락커 배정 완료");
    }

    private void importUniform(Row row, DataFormatter formatter, String memberId, Long gymId, List<String> messages) {
        LocalDate start = parseDate(row, 18, formatter);
        LocalDate end = parseDate(row, 19, formatter);
        if (start == null && end == null) return; // 운동복 정보 없음(선택)
        if (start == null || end == null) {
            messages.add("운동복 이용기간(시작/종료일)이 모두 있어야 등록됩니다.");
            return;
        }

        Membership m = new Membership();
        m.setMemberId(memberId);
        m.setType("ITEM");
        m.setStartDate(start);
        m.setEndDate(end);
        m.setRegType("NEW");
        String memo = str(row, 20, formatter);
        m.setMemo(memo != null && !memo.isBlank() ? memo : "타사 CRM 이관 등록(운동복)");
        memberMapper.insertMembership(m, gymId);
        messages.add("운동복 등록 완료");
    }

    // ── 셀 파싱 헬퍼 ──

    private boolean isRowBlank(Row row, DataFormatter formatter) {
        for (int i = 0; i < HEADERS.length; i++) {
            String v = str(row, i, formatter);
            if (v != null && !v.isBlank()) return false;
        }
        return true;
    }

    private String str(Row row, int idx, DataFormatter formatter) {
        Cell cell = row.getCell(idx);
        if (cell == null) return null;
        String v = formatter.formatCellValue(cell).trim();
        return v.isEmpty() ? null : v;
    }

    private String blankToNull(String v) {
        return (v == null || v.isBlank()) ? null : v;
    }

    private String normalizePhone(String phone) {
        if (phone == null) return null;
        String digits = phone.replaceAll("[^0-9]", "");
        return digits.isEmpty() ? null : digits;
    }

    private Integer intValOrNull(Row row, int idx, DataFormatter formatter) {
        String v = str(row, idx, formatter);
        if (v == null) return null;
        try {
            return (int) Double.parseDouble(v.replaceAll("[^0-9.\\-]", ""));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private int intVal(Row row, int idx, DataFormatter formatter, int defaultVal) {
        Integer v = intValOrNull(row, idx, formatter);
        return v != null ? v : defaultVal;
    }

    private LocalDate parseDate(Row row, int idx, DataFormatter formatter) {
        Cell cell = row.getCell(idx);
        if (cell == null) return null;
        if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
            return cell.getLocalDateTimeCellValue().toLocalDate();
        }
        String v = str(row, idx, formatter);
        if (v == null) return null;
        for (DateTimeFormatter fmt : DATE_FORMATS) {
            try {
                return LocalDate.parse(v, fmt);
            } catch (Exception ignored) {
                // try next format
            }
        }
        return null;
    }
}
