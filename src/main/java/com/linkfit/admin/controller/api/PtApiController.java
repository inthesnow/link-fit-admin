package com.linkfit.admin.controller.api;

import com.linkfit.admin.common.ApiResponse;
import com.linkfit.admin.domain.PtMember;
import com.linkfit.admin.mapper.MemberMapper;
import com.linkfit.admin.security.CrmUserDetails;
import com.linkfit.admin.service.MemberService;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/pt")
public class PtApiController {

    private static final Logger log = LoggerFactory.getLogger(PtApiController.class);

    private final MemberMapper memberMapper;
    private final MemberService memberService;

    public PtApiController(MemberMapper memberMapper, MemberService memberService) {
        this.memberMapper = memberMapper;
        this.memberService = memberService;
    }

    @GetMapping("/members")
    public ApiResponse<Map<String, Object>> listPtMembers(
            @RequestParam(defaultValue = "false") boolean lowStock,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal CrmUserDetails principal) {
        log.info("[Pt] GET /api/pt/members - lowStock={}, page={}", lowStock, page);
        Long gymId = principal.getGymId();
        int offset = page * size;
        List<PtMember> items = memberMapper.findPtMembers(lowStock, offset, size, gymId);
        long total = memberMapper.countPtMembers(lowStock, gymId);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("items", items);
        data.put("total", total);
        return ApiResponse.ok(data);
    }

    @GetMapping("/export")
    public void export(
            @RequestParam(defaultValue = "false") boolean lowStock,
            HttpServletResponse response,
            @AuthenticationPrincipal CrmUserDetails principal) throws IOException {
        log.info("[Pt] GET /api/pt/export - lowStock={}", lowStock);
        Long gymId = principal.getGymId();
        List<PtMember> members = memberMapper.findPtMembers(lowStock, 0, 100_000, gymId);

        String filename = "pt-members-" + LocalDate.now() + ".xlsx";
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");

        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("PT 회원");
            String[] headers = { "회원명", "전화번호", "등급", "담당트레이너", "구매PT", "서비스PT", "PT잔여합계" };
            Row hRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) hRow.createCell(i).setCellValue(headers[i]);
            int rowIdx = 1;
            for (PtMember m : members) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(m.getMemberName()   != null ? m.getMemberName()   : m.getMemberId());
                row.createCell(1).setCellValue(m.getMemberPhone()  != null ? m.getMemberPhone()  : "");
                row.createCell(2).setCellValue(m.getTier()         != null ? m.getTier()         : "");
                row.createCell(3).setCellValue(m.getTrainerName()  != null ? m.getTrainerName()  : "");
                row.createCell(4).setCellValue(m.getPurchasedPt());
                row.createCell(5).setCellValue(m.getServicePt());
                row.createCell(6).setCellValue(m.getPtRemaining());
            }
            wb.write(response.getOutputStream());
        }
    }

    // 구매 PT(pt_sessions_left)와 서비스 PT(service_pt_sessions_left)를 분리 조정한다.
    // 같은 API를 회원상세(members.html)와 PT 관리(pt.html) 양쪽에서 호출한다.
    @PutMapping("/members/{memberId}/sessions")
    public ApiResponse<Void> adjustSessions(@PathVariable String memberId,
                                             @RequestBody Map<String, Object> body,
                                             @AuthenticationPrincipal CrmUserDetails principal) {
        log.info("[Pt] PUT /api/pt/members/{memberId}/sessions - memberId={}, body={}", memberId, body);
        String target = (String) body.get("target");
        Object rawDelta = body.get("delta");
        if (!(rawDelta instanceof Number)) {
            return ApiResponse.error("delta는 숫자여야 합니다.");
        }
        int delta = ((Number) rawDelta).intValue();
        if (delta == 0) {
            return ApiResponse.error("조정할 횟수를 입력해주세요.");
        }
        Long gymId = principal.getGymId();
        if ("SERVICE".equals(target)) {
            memberMapper.adjustServicePtSessions(memberId, delta, gymId);
        } else if ("PURCHASED".equals(target)) {
            memberMapper.adjustPtSessions(memberId, delta, gymId);
        } else {
            return ApiResponse.error("알 수 없는 대상입니다.");
        }
        return ApiResponse.ok();
    }

    // PT는 특정 구매건 단위가 아니라 회원 전체 PT 잔여 풀(구매+서비스)을 그대로 이전한다.
    @PostMapping("/members/{memberId}/transfer")
    public ApiResponse<Void> transferPt(@PathVariable String memberId, @RequestBody Map<String, String> body,
                                         @AuthenticationPrincipal CrmUserDetails principal) {
        log.info("[Pt] POST /api/pt/members/{memberId}/transfer - memberId={}, body={}", memberId, body);
        String targetMemberId = body.get("targetMemberId");
        if (targetMemberId == null || targetMemberId.isBlank()) {
            return ApiResponse.error("이전받을 회원을 선택해주세요.");
        }
        if (memberId.equals(targetMemberId)) {
            return ApiResponse.error("본인에게는 이전할 수 없습니다.");
        }
        Long gymId = principal.getGymId();
        var source = memberMapper.findById(memberId, gymId).orElse(null);
        if (source == null) {
            return ApiResponse.error("회원을 찾을 수 없습니다.");
        }
        if (!memberMapper.existsInGym(targetMemberId, gymId)) {
            return ApiResponse.error("대상 회원을 찾을 수 없습니다.");
        }
        int purchased = source.getPtSessionsLeft() != null ? source.getPtSessionsLeft() : 0;
        int service = source.getServicePtSessionsLeft() != null ? source.getServicePtSessionsLeft() : 0;
        if (purchased + service <= 0) {
            return ApiResponse.error("이전할 PT 잔여 세션이 없습니다.");
        }
        memberService.transferPtSessions(memberId, targetMemberId, gymId);
        return ApiResponse.ok();
    }
}
