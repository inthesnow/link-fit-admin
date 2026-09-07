package com.linkfit.admin.controller.api;

import com.linkfit.admin.common.ApiResponse;
import com.linkfit.admin.domain.CrmMemberNote;
import com.linkfit.admin.domain.CrmMemberTag;
import com.linkfit.admin.domain.CrmMessage;
import com.linkfit.admin.domain.Member;
import com.linkfit.admin.domain.MemberTicket;
import com.linkfit.admin.domain.Staff;
import com.linkfit.admin.domain.TicketLog;
import com.linkfit.admin.mapper.CrmMessageMapper;
import com.linkfit.admin.security.CrmUserDetails;
import com.linkfit.admin.service.CrmMemberService;
import com.linkfit.admin.service.MemberImportService;
import com.linkfit.admin.service.MemberService;
import com.linkfit.admin.service.StaffService;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/members")
public class MemberApiController {

    private static final Logger log = LoggerFactory.getLogger(MemberApiController.class);

    private final MemberService memberService;
    private final CrmMemberService crmMemberService;
    private final StaffService staffService;
    private final MemberImportService memberImportService;
    private final CrmMessageMapper crmMessageMapper;

    public MemberApiController(MemberService memberService, CrmMemberService crmMemberService,
                                StaffService staffService, MemberImportService memberImportService,
                                CrmMessageMapper crmMessageMapper) {
        this.memberService    = memberService;
        this.crmMemberService = crmMemberService;
        this.staffService     = staffService;
        this.memberImportService = memberImportService;
        this.crmMessageMapper = crmMessageMapper;
    }

    // 회원 상세정보의 "담당 트레이너 변경" 드롭다운 전용 — /api/staff는 "staff"(직원 관리)
    // 카테고리로 2차 비밀번호 잠금 대상이라, 회원관리("members" 카테고리)만 잠금 해제한
    // 관리자가 회원 상세에서 트레이너를 배정하려 하면 423으로 막히는 문제가 있었음
    // (직원 관리를 잠근 계정에서 실제로 재현 확인함). id/name만 내려주는 최소 정보 엔드포인트를
    // members 카테고리 하위에 따로 둬서 우회.
    @GetMapping("/trainer-options")
    public ApiResponse<List<Map<String, String>>> trainerOptions(@AuthenticationPrincipal CrmUserDetails principal) {
        List<Staff> trainers = staffService.findTrainerOptions(principal.getGymId());
        List<Map<String, String>> options = trainers.stream()
                .map(t -> Map.of("id", t.getId(), "name", t.getName() != null ? t.getName() : t.getId()))
                .toList();
        return ApiResponse.ok(options);
    }

    // 회원관리 페이지 상단 집계 패널 — {total, valid, expired, expiring}
    @GetMapping("/summary")
    public ApiResponse<Map<String, Object>> summary(@AuthenticationPrincipal CrmUserDetails principal) {
        log.info("[Member] GET /api/members/summary");
        return ApiResponse.ok(memberService.summaryCounts(principal.getGymId()));
    }

    @GetMapping
    public ApiResponse<Map<String, Object>> list(
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(defaultValue = "") String status,
            @RequestParam(defaultValue = "") String tier,
            @RequestParam(required = false) List<String> trainerIds,
            @RequestParam(required = false) Integer minDaysLeft,
            @RequestParam(required = false) Integer maxDaysLeft,
            @RequestParam(required = false) Integer minPtRemaining,
            @RequestParam(required = false) Integer minAbsentDays,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal CrmUserDetails principal) {
        log.info("[Member] GET /api/members - keyword={}, status={}, trainerIds={}", keyword, status, trainerIds);
        Long gymId = principal.getGymId();
        List<Member> members = memberService.findAll(keyword, status, tier, gymId, trainerIds,
                minDaysLeft, maxDaysLeft, minPtRemaining, minAbsentDays, page, size);
        long total = memberService.count(keyword, status, tier, gymId, trainerIds,
                minDaysLeft, maxDaysLeft, minPtRemaining, minAbsentDays);
        return ApiResponse.ok(Map.of("members", members, "total", total, "page", page, "size", size));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Member>> get(@PathVariable String id,
                                                    @AuthenticationPrincipal CrmUserDetails principal) {
        log.info("[Member] GET /api/members/{id} - id={}", id);
        return memberService.findById(id, principal.getGymId())
            .map(m -> ResponseEntity.ok(ApiResponse.ok(m)))
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ApiResponse<Member> create(@RequestBody Member member,
                                       @AuthenticationPrincipal CrmUserDetails principal) {
        log.info("[Member] POST /api/members");
        if (member.getName() == null || member.getName().isBlank()
                || member.getPhone() == null || member.getPhone().isBlank()) {
            return ApiResponse.error("이름과 전화번호를 모두 입력해주세요.");
        }
        if (memberService.existsByNameAndPhone(member.getName(), member.getPhone())) {
            return ApiResponse.error("동일한 이름과 전화번호의 회원이 이미 존재합니다.");
        }
        return ApiResponse.ok(memberService.save(member, principal.getGymId()));
    }

    @PutMapping("/{id}")
    public ApiResponse<Member> update(@PathVariable String id, @RequestBody Member member,
                                       @AuthenticationPrincipal CrmUserDetails principal) {
        log.info("[Member] PUT /api/members/{id} - id={}", id);
        return ApiResponse.ok(memberService.update(id, member, principal.getGymId()));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable String id,
                                     @AuthenticationPrincipal CrmUserDetails principal) {
        log.info("[Member] DELETE /api/members/{id} - id={}", id);
        memberService.delete(id, principal.getGymId());
        return ApiResponse.ok();
    }

    @PatchMapping("/{id}/status")
    public ApiResponse<Void> updateStatus(@PathVariable String id, @RequestBody Map<String, String> body,
                                           @AuthenticationPrincipal CrmUserDetails principal) {
        log.info("[Member] PATCH /api/members/{id}/status - id={}", id);
        memberService.updateStatus(id, body.get("status"), principal.getGymId());
        return ApiResponse.ok();
    }

    @PatchMapping("/{id}/tier")
    public ApiResponse<Void> updateTier(@PathVariable String id, @RequestBody Map<String, String> body,
                                         @AuthenticationPrincipal CrmUserDetails principal) {
        log.info("[Member] PATCH /api/members/{id}/tier - id={}", id);
        memberService.updateTier(id, body.get("tier"), principal.getGymId());
        return ApiResponse.ok();
    }

    @PatchMapping("/{id}/member-type")
    public ApiResponse<Void> updateMemberType(@PathVariable String id, @RequestBody Map<String, String> body,
                                               @AuthenticationPrincipal CrmUserDetails principal) {
        log.info("[Member] PATCH /api/members/{id}/member-type - id={}", id);
        memberService.updateMemberType(id, body.get("memberType"), principal.getGymId());
        return ApiResponse.ok();
    }

    @PatchMapping("/{id}/role")
    public ApiResponse<Void> updateRole(@PathVariable String id, @RequestBody Map<String, String> body,
                                         @AuthenticationPrincipal CrmUserDetails principal) {
        log.info("[Member] PATCH /api/members/{id}/role - id={}, role={}", id, body.get("role"));
        memberService.updateRole(id, body.get("role"), principal.getGymId());
        return ApiResponse.ok();
    }

    @PostMapping("/{id}/freeze")
    public ApiResponse<Void> freeze(@PathVariable String id, @RequestBody Map<String, String> body,
                                     @AuthenticationPrincipal CrmUserDetails principal) {
        log.info("[Member] POST /api/members/{id}/freeze - id={}", id);
        memberService.freeze(id, body.get("startDate"), body.get("endDate"), principal.getGymId());
        return ApiResponse.ok();
    }

    @PatchMapping("/{id}/withdraw")
    public ApiResponse<Void> withdraw(@PathVariable String id,
                                       @AuthenticationPrincipal CrmUserDetails principal) {
        log.info("[Member] PATCH /api/members/{id}/withdraw - id={}", id);
        memberService.withdraw(id, principal.getGymId());
        return ApiResponse.ok();
    }

    // 담당 트레이너 지정 — user_profiles.trainer_id (앱이 실제로 사용하는 필드).
    // PUT /{id}/trainer(아래, crm_member_assignments 대상)와는 별개의 값이니 혼동 주의.
    @PatchMapping("/{id}/assigned-trainer")
    public ApiResponse<Void> updateAssignedTrainer(
            @PathVariable String id,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal CrmUserDetails principal) {
        log.info("[Member] PATCH /api/members/{id}/assigned-trainer - id={}", id);
        memberService.updateAssignedTrainer(id, body.get("trainerId"), principal.getGymId());
        return ApiResponse.ok();
    }

    @GetMapping("/{id}/memberships")
    public ApiResponse<List<com.linkfit.admin.domain.Membership>> getMemberships(
            @PathVariable String id,
            @AuthenticationPrincipal CrmUserDetails principal) {
        log.info("[Member] GET /api/members/{id}/memberships - id={}", id);
        return ApiResponse.ok(memberService.findMemberships(id, principal.getGymId()));
    }

    @PostMapping("/{id}/memberships")
    public ApiResponse<Void> addMembership(@PathVariable String id,
                                           @RequestBody com.linkfit.admin.domain.Membership membership,
                                           @AuthenticationPrincipal CrmUserDetails principal) {
        log.info("[Member] POST /api/members/{id}/memberships - id={}", id);
        membership.setMemberId(id);
        memberService.addMembership(membership, principal.getGymId());
        return ApiResponse.ok();
    }

    @GetMapping("/{id}/tickets")
    public ApiResponse<List<MemberTicket>> getTickets(@PathVariable String id) {
        log.info("[Member] GET /api/members/{id}/tickets - id={}", id);
        return ApiResponse.ok(memberService.findTickets(id));
    }

    // 활성 회원 전체의 ONE_POINT/FEEDBACK 잔량 합계 (구독권/티켓 관리 페이지 상단 요약 카드용)
    @GetMapping("/tickets/totals")
    public ApiResponse<Map<String, Object>> ticketTotals(@AuthenticationPrincipal CrmUserDetails principal) {
        log.info("[Member] GET /api/members/tickets/totals");
        return ApiResponse.ok(memberService.ticketTotals(principal.getGymId()));
    }

    @PostMapping("/{id}/tickets/charge")
    public ApiResponse<Void> chargeTicket(@PathVariable String id, @RequestBody Map<String, Object> body,
                                           @AuthenticationPrincipal CrmUserDetails principal) {
        log.info("[Member] POST /api/members/{id}/tickets/charge - id={}", id);
        String ticketType  = (String) body.get("ticketType");
        Object rawAmount   = body.get("amount");
        if (!(rawAmount instanceof Number)) {
            return ApiResponse.error("amount는 숫자여야 합니다.");
        }
        int amount          = ((Number) rawAmount).intValue();
        String description = (String) body.get("description");
        memberService.chargeTicket(id, ticketType, amount, description, principal.getGymId());
        return ApiResponse.ok();
    }

    // 티켓 지급/차감 사용내역 (구독권/티켓 관리 > 사용내역 탭)
    @GetMapping("/tickets/logs")
    public ApiResponse<Map<String, Object>> ticketLogs(
            @RequestParam(defaultValue = "") String ticketType,
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal CrmUserDetails principal) {
        log.info("[Member] GET /api/members/tickets/logs - ticketType={}, keyword={}", ticketType, keyword);
        Long gymId = principal.getGymId();
        List<TicketLog> logs = memberService.findTicketLogs(gymId, ticketType, keyword, page, size);
        long total = memberService.countTicketLogs(gymId, ticketType, keyword);
        return ApiResponse.ok(Map.of("logs", logs, "total", total));
    }

    // ── CRM Sector 2 ──────────────────────────────────────────

    @GetMapping("/{id}/notes")
    public ApiResponse<List<CrmMemberNote>> getNotes(
            @PathVariable String id,
            @AuthenticationPrincipal CrmUserDetails principal) {
        log.info("[Member] GET /api/members/{id}/notes - id={}", id);
        return ApiResponse.ok(crmMemberService.findNotes(id, principal.getGymId()));
    }

    @PostMapping("/{id}/notes")
    public ApiResponse<CrmMemberNote> addNote(
            @PathVariable String id,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal CrmUserDetails principal) {
        log.info("[Member] POST /api/members/{id}/notes - id={}", id);
        return ApiResponse.ok(crmMemberService.addNote(
                id, principal.getGymId(), principal.getId(), body.get("content")));
    }

    // 회원 상세의 "쪽지" 탭 — 관리자<->이 회원 간 전체 대화(crm_messages, 양방향).
    // 회원 쪽(인앱)에서는 lof-backend의 별도 /api/gym-messages가 같은 crm_messages
    // 테이블을 gym_id 기준으로 읽고 쓴다 — 이 화면과 앱 화면은 같은 대화를 공유한다.
    @GetMapping("/{id}/messages")
    public ApiResponse<List<CrmMessage>> getMessages(
            @PathVariable String id,
            @AuthenticationPrincipal CrmUserDetails principal) {
        log.info("[Member] GET /api/members/{id}/messages - id={}", id);
        List<CrmMessage> thread = crmMessageMapper.findThreadWithMember(principal.getGymId(), id);
        crmMessageMapper.markMemberMessagesRead(principal.getGymId(), id);
        return ApiResponse.ok(thread);
    }

    @PostMapping("/{id}/messages")
    public ApiResponse<CrmMessage> sendMessage(
            @PathVariable String id,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal CrmUserDetails principal) {
        log.info("[Member] POST /api/members/{id}/messages - id={}", id);
        String content = body.get("content");
        if (content == null || content.isBlank()) {
            return ApiResponse.error("내용을 입력해주세요.");
        }
        CrmMessage message = new CrmMessage();
        message.setId(UUID.randomUUID().toString());
        message.setGymId(principal.getGymId());
        message.setSenderType("admin");
        message.setSenderId(principal.getId());
        message.setReceiverType("member");
        message.setReceiverId(id);
        message.setContent(content);
        message.setNotice(false);
        crmMessageMapper.insert(message);
        return ApiResponse.ok(message);
    }

    @GetMapping("/{id}/tags")
    public ApiResponse<List<CrmMemberTag>> getTags(
            @PathVariable String id,
            @AuthenticationPrincipal CrmUserDetails principal) {
        log.info("[Member] GET /api/members/{id}/tags - id={}", id);
        return ApiResponse.ok(crmMemberService.findTags(id, principal.getGymId()));
    }

    @PostMapping("/{id}/tags")
    public ApiResponse<CrmMemberTag> addTag(
            @PathVariable String id,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal CrmUserDetails principal) {
        log.info("[Member] POST /api/members/{id}/tags - id={}", id);
        return ApiResponse.ok(crmMemberService.addTag(
                id, principal.getGymId(), body.get("tag"), body.get("color")));
    }

    @DeleteMapping("/{id}/tags/{tagId}")
    public ApiResponse<Void> deleteTag(
            @PathVariable String id,
            @PathVariable String tagId,
            @AuthenticationPrincipal CrmUserDetails principal) {
        log.info("[Member] DELETE /api/members/{id}/tags/{tagId} - id={}, tagId={}", id, tagId);
        crmMemberService.deleteTag(tagId, principal.getGymId());
        return ApiResponse.ok();
    }

    @GetMapping("/export")
    public void export(
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(defaultValue = "") String status,
            @RequestParam(defaultValue = "") String tier,
            @RequestParam(required = false) List<String> trainerIds,
            HttpServletResponse response,
            @AuthenticationPrincipal CrmUserDetails principal) throws IOException {
        log.info("[Member] GET /api/members/export - keyword={}, status={}", keyword, status);
        List<Member> members = memberService.findAll(keyword, status, tier, principal.getGymId(), trainerIds,
                null, null, null, null, 0, 100_000);

        String filename = "members-" + LocalDate.now() + ".xlsx";
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");

        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("회원 목록");
            String[] headers = { "회원ID", "이름", "이메일", "연락처", "성별", "생년월일", "상태", "가입일", "회원권 만료일", "회원유형", "등급" };
            Row hRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) hRow.createCell(i).setCellValue(headers[i]);
            int rowIdx = 1;
            for (Member m : members) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(m.getId()           != null ? m.getId()                   : "");
                row.createCell(1).setCellValue(m.getName()         != null ? m.getName()                 : "");
                row.createCell(2).setCellValue(m.getEmail()        != null ? m.getEmail()                : "");
                row.createCell(3).setCellValue(m.getPhone()        != null ? m.getPhone()                : "");
                row.createCell(4).setCellValue(m.getGender()       != null ? m.getGender()               : "");
                row.createCell(5).setCellValue(m.getBirthDate()    != null ? m.getBirthDate().toString()  : "");
                row.createCell(6).setCellValue(m.getStatus()       != null ? m.getStatus()               : "");
                row.createCell(7).setCellValue(m.getJoinDate()     != null ? m.getJoinDate().toString()   : "");
                row.createCell(8).setCellValue(m.getMembershipEnd() != null ? m.getMembershipEnd().toString() : "");
                row.createCell(9).setCellValue(m.getMemberType()   != null ? m.getMemberType()           : "");
                row.createCell(10).setCellValue(m.getTier()        != null ? m.getTier()                 : "");
            }
            wb.write(response.getOutputStream());
        }
    }

    // ── 회원 일괄 등록(엑셀) — 타사 CRM에서 이관해오는 순수 헬스장 정보 전용 ──
    @GetMapping("/import/template")
    public ResponseEntity<byte[]> importTemplate() {
        log.info("[Member] GET /api/members/import/template");
        byte[] bytes = memberImportService.generateTemplate();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"member-import-template.xlsx\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(bytes);
    }

    @PostMapping("/import")
    public ApiResponse<List<Map<String, Object>>> importMembers(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal CrmUserDetails principal) {
        log.info("[Member] POST /api/members/import - filename={}, size={}", file.getOriginalFilename(), file.getSize());
        if (file.isEmpty()) {
            return ApiResponse.error("업로드할 파일을 선택해주세요.");
        }
        try {
            List<Map<String, Object>> results = memberImportService.importExcel(file.getInputStream(), principal.getGymId());
            return ApiResponse.ok(results);
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(e.getMessage());
        } catch (IOException e) {
            return ApiResponse.error("파일을 읽을 수 없습니다.");
        }
    }

    @PutMapping("/{id}/trainer")
    public ApiResponse<Void> assignTrainer(
            @PathVariable String id,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal CrmUserDetails principal) {
        log.info("[Member] PUT /api/members/{id}/trainer - id={}", id);
        crmMemberService.assignTrainer(id, principal.getGymId(), body.get("trainerId"));
        return ApiResponse.ok();
    }
}
