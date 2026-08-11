package com.linkfit.admin.controller.api;

import com.linkfit.admin.common.ApiResponse;
import com.linkfit.admin.domain.FeedbackRequest;
import com.linkfit.admin.security.CrmUserDetails;
import com.linkfit.admin.service.FeedbackService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/feedback")
public class FeedbackApiController {

    private static final Logger log = LoggerFactory.getLogger(FeedbackApiController.class);

    private final FeedbackService feedbackService;

    public FeedbackApiController(FeedbackService feedbackService) {
        this.feedbackService = feedbackService;
    }

    // ── Sector 10: 피드백 요청 관리 ─────────────────────────

    @GetMapping("/requests")
    public ApiResponse<Map<String, Object>> listRequests(
            @RequestParam(defaultValue = "") String status,
            @RequestParam(defaultValue = "") String trainerId,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal CrmUserDetails principal) {
        log.info("[Feedback] GET /api/feedback/requests - status={}, trainerId={}", status, trainerId);
        Long gymId = principal.getGymId();
        List<FeedbackRequest> list = feedbackService.findRequests(gymId, status, trainerId, page, size);
        long total = feedbackService.countRequests(gymId, status, trainerId);
        return ApiResponse.ok(Map.of("requests", list, "total", total, "page", page, "size", size));
    }

    @GetMapping("/requests/{id}")
    public ResponseEntity<ApiResponse<FeedbackRequest>> getRequest(@PathVariable String id) {
        log.info("[Feedback] GET /api/feedback/requests/{id} - id={}", id);
        return feedbackService.findRequestById(id)
                .map(r -> ResponseEntity.ok(ApiResponse.ok(r)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PatchMapping("/requests/{id}/assign")
    public ApiResponse<Void> assignRequestTrainer(
            @PathVariable String id,
            @RequestBody Map<String, String> body) {
        log.info("[Feedback] PATCH /api/feedback/requests/{id}/assign - id={}", id);
        feedbackService.assignRequestTrainer(id, body.get("trainerId"));
        return ApiResponse.ok();
    }

    @PostMapping("/requests/{id}/respond")
    public ApiResponse<Void> respond(
            @PathVariable String id,
            @RequestBody Map<String, String> body) {
        log.info("[Feedback] POST /api/feedback/requests/{id}/respond - id={}", id);
        feedbackService.respondToRequest(id, body.get("response"));
        return ApiResponse.ok();
    }

    @PatchMapping("/requests/{id}/status")
    public ApiResponse<Void> updateRequestStatus(
            @PathVariable String id,
            @RequestBody Map<String, String> body) {
        log.info("[Feedback] PATCH /api/feedback/requests/{id}/status - id={}", id);
        feedbackService.updateRequestStatus(id, body.get("status"));
        return ApiResponse.ok();
    }
}
