package com.linkfit.admin;

import com.linkfit.admin.domain.Member;
import com.linkfit.admin.domain.Membership;
import com.linkfit.admin.mapper.LockerMapper;
import com.linkfit.admin.mapper.MemberMapper;
import com.linkfit.admin.service.mybatis.MyBatisMemberImportService;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MyBatisMemberImportServiceTest {

    @Mock MemberMapper memberMapper;
    @Mock LockerMapper lockerMapper;

    MyBatisMemberImportService service;

    @BeforeEach
    void setUp() {
        service = new MyBatisMemberImportService(memberMapper, lockerMapper);
    }

    private byte[] buildWorkbook(String[] dataRow) throws Exception {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet();
            sheet.createRow(0); // header
            sheet.createRow(1); // example row (skipped by importExcel)
            Row row = sheet.createRow(2);
            for (int i = 0; i < dataRow.length; i++) {
                if (dataRow[i] != null) row.createCell(i).setCellValue(dataRow[i]);
            }
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            wb.write(bos);
            return bos.toByteArray();
        }
    }

    @Test
    void importExcel_blankEndDate_registersAsAlreadyExpired() throws Exception {
        String[] row = new String[21];
        row[0] = "홍길동";
        row[1] = "010-1234-5678";
        row[5] = "회원권";        // 회원권구분
        row[6] = "3개월 회원권";   // 상품명
        row[7] = "2020-01-01";   // 이용시작일
        row[8] = null;           // 이용종료일 — 비어있음

        when(memberMapper.findIdByNameAndPhone("홍길동", "01012345678")).thenReturn(Optional.empty());

        byte[] xlsx = buildWorkbook(row);
        List<Map<String, Object>> results = service.importExcel(new ByteArrayInputStream(xlsx), 1L);

        assertThat(results).hasSize(1);
        Map<String, Object> result = results.get(0);
        assertThat(result.get("success")).isEqualTo(true);
        @SuppressWarnings("unchecked")
        List<String> messages = (List<String>) result.get("messages");
        assertThat(messages).anyMatch(m -> m.contains("이미 만료된 회원으로 분류"));

        ArgumentCaptor<Membership> captor = ArgumentCaptor.forClass(Membership.class);
        verify(memberMapper).insertMembership(captor.capture(), any());
        Membership saved = captor.getValue();
        assertThat(saved.getEndDate()).isBefore(LocalDate.now());
        assertThat(saved.getStartDate()).isEqualTo(LocalDate.of(2020, 1, 1));
        // status IS NULL 조건에서 자동으로 빠지도록 EXPIRED_UNKNOWN 마킹돼야 함(유효 이용권 집계 제외)
        assertThat(saved.getStatus()).isEqualTo("EXPIRED_UNKNOWN");

        // 엑셀엔 가입일 컬럼이 없어 created_at을 가입일처럼 보여주면 안 됨 — 화면엔 공란 표시되도록
        ArgumentCaptor<Member> memberCaptor = ArgumentCaptor.forClass(Member.class);
        verify(memberMapper).insertProfile(memberCaptor.capture());
        assertThat(memberCaptor.getValue().isJoinDateUnknown()).isTrue();
    }

    @Test
    void importExcel_ptTypeBlankEndDate_stillUsesUnlimitedDuration() throws Exception {
        String[] row = new String[21];
        row[0] = "김철수";
        row[1] = "010-9999-8888";
        row[5] = "PT";
        row[6] = "PT 10회";
        row[7] = "2026-08-01";
        row[8] = null;
        row[9] = "10"; // PT횟수

        when(memberMapper.findIdByNameAndPhone("김철수", "01099998888")).thenReturn(Optional.empty());

        byte[] xlsx = buildWorkbook(row);
        List<Map<String, Object>> results = service.importExcel(new ByteArrayInputStream(xlsx), 1L);

        assertThat(results).hasSize(1);
        @SuppressWarnings("unchecked")
        List<String> messages = (List<String>) results.get(0).get("messages");
        assertThat(messages).noneMatch(m -> m.contains("이미 만료된 회원으로 분류"));

        ArgumentCaptor<Membership> captor = ArgumentCaptor.forClass(Membership.class);
        verify(memberMapper).insertMembership(captor.capture(), any());
        assertThat(captor.getValue().getEndDate()).isAfter(LocalDate.now());
        assertThat(captor.getValue().getStatus()).isNull();
    }
}
