package com.linkfit.admin.controller.api;

import com.linkfit.admin.common.ApiResponse;
import com.linkfit.admin.mapper.FeedbackRequestMapper;
import com.linkfit.admin.mapper.MemberMapper;
import com.linkfit.admin.mapper.ReRegistrationMapper;
import com.linkfit.admin.security.CrmUserDetails;
import com.linkfit.admin.service.DashboardService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardApiController {

    private static final Logger log = LoggerFactory.getLogger(DashboardApiController.class);

    private final DashboardService dashboardService;
    private final MemberMapper memberMapper;
    private final FeedbackRequestMapper feedbackRequestMapper;
    private final ReRegistrationMapper reRegistrationMapper;

    public DashboardApiController(DashboardService dashboardService,
                                   MemberMapper memberMapper,
                                   FeedbackRequestMapper feedbackRequestMapper,
                                   ReRegistrationMapper reRegistrationMapper) {
        this.dashboardService = dashboardService;
        this.memberMapper = memberMapper;
        this.feedbackRequestMapper = feedbackRequestMapper;
        this.reRegistrationMapper = reRegistrationMapper;
    }

    @GetMapping("/members")
    public ApiResponse<Map<String, Object>> memberStats(
            @RequestParam(defaultValue = "") String date,
            @RequestParam(defaultValue = "daily") String period,
            @RequestParam(defaultValue = "") String type) {
        log.info("[Dashboard] GET /api/dashboard/members - date={}, period={}", date, period);
        return ApiResponse.ok(dashboardService.memberStats(date, period));
    }

    @GetMapping("/consults")
    public ApiResponse<Map<String, Object>> consultStats(
            @RequestParam(defaultValue = "") String date,
            @RequestParam(defaultValue = "daily") String period) {
        log.info("[Dashboard] GET /api/dashboard/consults - date={}, period={}", date, period);
        return ApiResponse.ok(dashboardService.consultStats(date, period));
    }

    @GetMapping("/classes")
    public ApiResponse<Map<String, Object>> classStats(
            @RequestParam(defaultValue = "") String date,
            @RequestParam(defaultValue = "daily") String period,
            @RequestParam(defaultValue = "") String type) {
        log.info("[Dashboard] GET /api/dashboard/classes - date={}, period={}", date, period);
        return ApiResponse.ok(dashboardService.classStats(date, period));
    }

    @GetMapping("/revenue")
    public ApiResponse<Map<String, Object>> revenueStats(
            @RequestParam(defaultValue = "") String date,
            @RequestParam(defaultValue = "daily") String period) {
        log.info("[Dashboard] GET /api/dashboard/revenue - date={}, period={}", date, period);
        return ApiResponse.ok(dashboardService.revenueStats(date, period));
    }

    @GetMapping("/revenue/{category}")
    public ApiResponse<Map<String, Object>> revenueDetail(
            @PathVariable String category,
            @RequestParam(defaultValue = "") String date,
            @RequestParam(defaultValue = "daily") String period) {
        log.info("[Dashboard] GET /api/dashboard/revenue/{category} - category={}, date={}, period={}", category, date, period);
        return ApiResponse.ok(dashboardService.revenueDetail(category, date, period));
    }

    @GetMapping("/attendance")
    public ApiResponse<Map<String, Object>> attendanceStats(
            @RequestParam(defaultValue = "") String date,
            @RequestParam(defaultValue = "daily") String period,
            @RequestParam(defaultValue = "") String type) {
        log.info("[Dashboard] GET /api/dashboard/attendance - date={}, period={}", date, period);
        return ApiResponse.ok(dashboardService.attendanceStats(date, period, type));
    }

    @GetMapping("/crm-summary")
    public ApiResponse<Map<String, Object>> crmSummary(
            @AuthenticationPrincipal CrmUserDetails principal) {
        log.info("[Dashboard] GET /api/dashboard/crm-summary");
        Long gymId = (principal != null) ? principal.getGymId() : 1L;
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("expiringCount",        memberMapper.countExpiringMemberships(30, gymId));
        data.put("pendingFeedback",       feedbackRequestMapper.count(gymId, "pending", null));
        data.put("pendingReregistration", reRegistrationMapper.countByStatus(gymId, "pending"));
        return ApiResponse.ok(data);
    }

    @GetMapping("/expiring")
    public ApiResponse<Map<String, Object>> expiring(
            @RequestParam(defaultValue = "30") int days,
            @AuthenticationPrincipal CrmUserDetails principal) {
        log.info("[Dashboard] GET /api/dashboard/expiring - days={}", days);
        Long gymId = (principal != null) ? principal.getGymId() : 1L;
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("count", memberMapper.countExpiringMemberships(days, gymId));
        data.put("days", days);
        return ApiResponse.ok(data);
    }

    @GetMapping("/app-usage")
    public ApiResponse<Map<String, Object>> appUsage(@RequestParam(defaultValue = "30") int days) {
        log.info("[Dashboard] GET /api/dashboard/app-usage - days={}", days);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("count", dashboardService.appUsageCount(days));
        data.put("days", days);
        return ApiResponse.ok(data);
    }

    @GetMapping("/routine-compliance")
    public ApiResponse<Map<String, Object>> routineCompliance(@RequestParam(defaultValue = "30") int days) {
        log.info("[Dashboard] GET /api/dashboard/routine-compliance - days={}", days);
        Map<String, Object> data = new LinkedHashMap<>(dashboardService.routineComplianceStats(days));
        data.put("days", days);
        return ApiResponse.ok(data);
    }

    @GetMapping("/reregistration-summary")
    public ApiResponse<Map<String, Object>> reregistrationSummary(
            @RequestParam(defaultValue = "30") int days,
            @AuthenticationPrincipal CrmUserDetails principal) {
        log.info("[Dashboard] GET /api/dashboard/reregistration-summary - days={}", days);
        Long gymId = (principal != null) ? principal.getGymId() : 1L;
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("pending",     reRegistrationMapper.countByStatusInPeriod(gymId, "pending", days));
        data.put("in_progress", reRegistrationMapper.countByStatusInPeriod(gymId, "in_progress", days));
        data.put("hold",        reRegistrationMapper.countByStatusInPeriod(gymId, "hold", days));
        data.put("days", days);
        return ApiResponse.ok(data);
    }
}
