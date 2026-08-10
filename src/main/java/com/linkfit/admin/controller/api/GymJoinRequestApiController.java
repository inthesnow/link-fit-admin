package com.linkfit.admin.controller.api;

import com.linkfit.admin.common.ApiResponse;
import com.linkfit.admin.domain.GymJoinRequest;
import com.linkfit.admin.domain.GymJoinRequestLog;
import com.linkfit.admin.security.CrmUserDetails;
import com.linkfit.admin.service.GymJoinRequestService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

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
