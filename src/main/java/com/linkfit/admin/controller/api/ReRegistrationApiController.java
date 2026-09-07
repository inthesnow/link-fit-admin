package com.linkfit.admin.controller.api;

import com.linkfit.admin.common.ApiResponse;
import com.linkfit.admin.domain.CrmReregistrationNote;
import com.linkfit.admin.domain.ReRegistration;
import com.linkfit.admin.security.CrmUserDetails;
import com.linkfit.admin.service.ReRegistrationService;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/reregistration")
public class ReRegistrationApiController {

    private static final Logger log = LoggerFactory.getLogger(ReRegistrationApiController.class);

    private final ReRegistrationService service;

    public ReRegistrationApiController(ReRegistrationService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<Map<String, Object>> list(
            @RequestParam(defaultValue = "") String status,
            @RequestParam(defaultValue = "") String reason,
            @RequestParam(required = false) String expiryStatus,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal CrmUserDetails principal) {
        log.info("[ReRegistration] GET /api/reregistration - status={}, reason={}, expiryStatus={}, startDate={}, endDate={}",
                status, reason, expiryStatus, startDate, endDate);
        Long gymId = principal.getGymId();
        List<ReRegistration> list = service.findAll(gymId, status, reason, expiryStatus, startDate, endDate, page, size);
        long total = service.count(gymId, status, reason, expiryStatus, startDate, endDate);
        return ApiResponse.ok(Map.of("items", list, "total", total, "page", page, "size", size));
    }

    @GetMapping("/export")
    public void export(
            @RequestParam(defaultValue = "") String status,
            @RequestParam(defaultValue = "") String reason,
            @RequestParam(required = false) String expiryStatus,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            HttpServletResponse response,
            @AuthenticationPrincipal CrmUserDetails principal) throws IOException {
        log.info("[ReRegistration] GET /api/reregistration/export - expiryStatus={}", expiryStatus);
        Long gymId = principal.getGymId();
        List<ReRegistration> list = service.findAll(gymId, status, reason, expiryStatus, startDate, endDate, 0, 100_000);

        String filename = "reregistration-" + LocalDate.now() + ".xlsx";
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");

        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("만료 및 예정 회원");
            String[] headers = { "회원명", "전화번호", "만료상태", "만료일", "등록상품", "구독권등급" };
            Row hRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) hRow.createCell(i).setCellValue(headers[i]);
            int rowIdx = 1;
            for (ReRegistration r : list) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(r.getMemberName()  != null ? r.getMemberName()  : r.getMemberId());
                row.createCell(1).setCellValue(r.getMemberPhone() != null ? r.getMemberPhone() : "");
                row.createCell(2).setCellValue(expiryLabel(r.getMembershipEnd()));
                row.createCell(3).setCellValue(r.getMembershipEnd() != null ? r.getMembershipEnd().toString() : "");
                row.createCell(4).setCellValue(r.getProductName()  != null ? r.getProductName()  : "");
                row.createCell(5).setCellValue(r.getTier()         != null ? r.getTier()         : "");
            }
            wb.write(response.getOutputStream());
        }
    }

    // gym-requests.html/members.html의 expiryInfo()와 동일한 만료/만료예정(30일)/유효 판정
    private String expiryLabel(LocalDate end) {
        if (end == null) return "만료";
        long diff = ChronoUnit.DAYS.between(LocalDate.now(), end);
        if (diff < 0) return "만료";
        if (diff <= 30) return "만료예정";
        return "유효";
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ReRegistration>> get(@PathVariable String id,
                                                           @AuthenticationPrincipal CrmUserDetails principal) {
        log.info("[ReRegistration] GET /api/reregistration/{id} - id={}", id);
        return service.findById(id, principal.getGymId())
                .map(r -> ResponseEntity.ok(ApiResponse.ok(r)))
                .orElse(ResponseEntity.notFound().build());
    }

    // 메모(스택형) — 회원 메모(crm_member_notes)와 동일한 패턴. 예전엔 상태 변경(pending/in_progress/
    // success/failed/hold)과 메모 덮어쓰기가 있었는데, 상태 변경 기능은 삭제하고 메모는 시간순으로
    // 쌓이는 방식으로 교체함(2026-08-25, "만료 및 예정 회원 관리" 개편).
    @GetMapping("/{id}/notes")
    public ApiResponse<List<CrmReregistrationNote>> getNotes(
            @PathVariable String id,
            @AuthenticationPrincipal CrmUserDetails principal) {
        log.info("[ReRegistration] GET /api/reregistration/{id}/notes - id={}", id);
        return ApiResponse.ok(service.findNotes(id, principal.getGymId()));
    }

    @PostMapping("/{id}/notes")
    public ApiResponse<CrmReregistrationNote> addNote(
            @PathVariable String id,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal CrmUserDetails principal) {
        log.info("[ReRegistration] POST /api/reregistration/{id}/notes - id={}", id);
        return ApiResponse.ok(service.addNote(id, principal.getGymId(), principal.getId(), body.get("content")));
    }

    @PatchMapping("/{id}/assign")
    public ApiResponse<Void> assign(
            @PathVariable String id,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal CrmUserDetails principal) {
        log.info("[ReRegistration] PATCH /api/reregistration/{id}/assign - id={}", id);
        service.assign(id, body.get("assignedTo"), principal.getGymId());
        return ApiResponse.ok();
    }

    @PostMapping("/auto-classify")
    public ApiResponse<Map<String, Object>> autoClassify(
            @AuthenticationPrincipal CrmUserDetails principal) {
        log.info("[ReRegistration] POST /api/reregistration/auto-classify");
        int created = service.autoClassify(principal.getGymId());
        return ApiResponse.ok(Map.of("created", created));
    }

    @GetMapping("/summary")
    public ApiResponse<Map<String, Integer>> summary(
            @AuthenticationPrincipal CrmUserDetails principal) {
        log.info("[ReRegistration] GET /api/reregistration/summary");
        return ApiResponse.ok(service.statusSummary(principal.getGymId()));
    }

    @GetMapping("/membership-summary")
    public ApiResponse<Map<String, Object>> membershipSummary(
            @AuthenticationPrincipal CrmUserDetails principal) {
        log.info("[ReRegistration] GET /api/reregistration/membership-summary");
        return ApiResponse.ok(service.membershipSummary(principal.getGymId()));
    }
}
