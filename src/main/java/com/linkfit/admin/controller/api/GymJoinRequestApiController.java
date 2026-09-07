package com.linkfit.admin.controller.api;

import com.linkfit.admin.common.ApiResponse;
import com.linkfit.admin.domain.GymJoinRequest;
import com.linkfit.admin.domain.GymJoinRequestLog;
import com.linkfit.admin.security.CrmUserDetails;
import com.linkfit.admin.service.GymJoinRequestService;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/gym-join-requests")
public class GymJoinRequestApiController {

    private static final Logger log = LoggerFactory.getLogger(GymJoinRequestApiController.class);

    private final GymJoinRequestService service;

    public GymJoinRequestApiController(GymJoinRequestService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<Map<String, Object>> list(
            @RequestParam(defaultValue = "") String status,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal CrmUserDetails principal) {
        log.info("[GymJoinRequest] GET /api/gym-join-requests - status={}", status);
        Long gymId = principal.getGymId();
        List<GymJoinRequest> list = service.findAll(gymId, status, page, size);
        long total = service.count(gymId, status);
        return ApiResponse.ok(Map.of("items", list, "total", total, "page", page, "size", size));
    }

    // 요약 카드 "승인됨" 전용 카운트 — 목록/페이징에 쓰이는 일반 count(status=APPROVED)와는 별개.
    // CRM 일괄등록으로 만들어져 앱 로그인 이력이 없는 회원까지 포함되는 것을 막기 위해
    // user_auth(실제 앱 가입 인증 정보)가 있는 사람만 센다.
    @GetMapping("/approved-app-count")
    public ApiResponse<Long> approvedAppCount(@AuthenticationPrincipal CrmUserDetails principal) {
        log.info("[GymJoinRequest] GET /api/gym-join-requests/approved-app-count");
        return ApiResponse.ok(service.countApprovedAppUsers(principal.getGymId()));
    }

    private static final Map<String, String> STATUS_LABEL = Map.of(
            "PENDING", "승인 대기", "APPROVED", "승인됨", "REJECTED", "거절됨");
    private static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @GetMapping("/export")
    public void export(
            @RequestParam(defaultValue = "") String status,
            HttpServletResponse response,
            @AuthenticationPrincipal CrmUserDetails principal) throws IOException {
        log.info("[GymJoinRequest] GET /api/gym-join-requests/export - status={}", status);
        Long gymId = principal.getGymId();
        List<GymJoinRequest> list = service.findAll(gymId, status, 0, 100_000);

        String filename = "gym-join-requests-" + LocalDate.now() + ".xlsx";
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");

        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("헬스장 가입 승인");
            String[] headers = { "회원명", "전화번호", "지점명", "상태", "요청일시" };
            Row hRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) hRow.createCell(i).setCellValue(headers[i]);
            int rowIdx = 1;
            for (GymJoinRequest r : list) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(r.getMemberName()  != null ? r.getMemberName()  : r.getMemberId());
                row.createCell(1).setCellValue(r.getMemberPhone() != null ? r.getMemberPhone() : "");
                row.createCell(2).setCellValue(r.getGymName()     != null ? r.getGymName()     : "");
                row.createCell(3).setCellValue(STATUS_LABEL.getOrDefault(r.getStatus(), r.getStatus()));
                row.createCell(4).setCellValue(r.getRequestedAt() != null ? r.getRequestedAt().format(DATETIME_FMT) : "");
            }
            wb.write(response.getOutputStream());
        }
    }

    @GetMapping("/{id}/logs")
    public ApiResponse<List<GymJoinRequestLog>> logs(@PathVariable Long id,
                                                       @AuthenticationPrincipal CrmUserDetails principal) {
        log.info("[GymJoinRequest] GET /api/gym-join-requests/{}/logs", id);
        GymJoinRequest req = service.findById(id).orElse(null);
        if (req == null || !req.getGymId().equals(principal.getGymId())) {
            return ApiResponse.error("요청을 찾을 수 없습니다.");
        }
        return ApiResponse.ok(service.findLogs(req.getMemberId(), req.getGymId()));
    }

    @PostMapping("/{id}/approve")
    public ApiResponse<Void> approve(@PathVariable Long id, @AuthenticationPrincipal CrmUserDetails principal) {
        log.info("[GymJoinRequest] POST /api/gym-join-requests/{}/approve", id);
        GymJoinRequest req = service.findById(id).orElse(null);
        if (req == null || !req.getGymId().equals(principal.getGymId())) {
            return ApiResponse.error("요청을 찾을 수 없습니다.");
        }
        if (!"PENDING".equals(req.getStatus())) {
            return ApiResponse.error("대기 중인 요청만 승인할 수 있습니다.");
        }
        service.approve(id, principal.getId());
        return ApiResponse.ok();
    }

    @PostMapping("/{id}/reject")
    public ApiResponse<Void> reject(@PathVariable Long id, @RequestBody(required = false) Map<String, String> body,
                                     @AuthenticationPrincipal CrmUserDetails principal) {
        log.info("[GymJoinRequest] POST /api/gym-join-requests/{}/reject", id);
        GymJoinRequest req = service.findById(id).orElse(null);
        if (req == null || !req.getGymId().equals(principal.getGymId())) {
            return ApiResponse.error("요청을 찾을 수 없습니다.");
        }
        if (!"PENDING".equals(req.getStatus())) {
            return ApiResponse.error("대기 중인 요청만 거절할 수 있습니다.");
        }
        String memo = body == null ? null : body.get("memo");
        service.reject(id, principal.getId(), (memo == null || memo.isBlank()) ? null : memo.trim());
        return ApiResponse.ok();
    }
}
