package com.linkfit.admin.controller.api;

import com.linkfit.admin.common.ApiResponse;
import com.linkfit.admin.domain.Attendance;
import com.linkfit.admin.domain.MemberFreeze;
import com.linkfit.admin.mapper.AttendanceMapper;
import com.linkfit.admin.security.CrmUserDetails;
import com.linkfit.admin.service.AttendanceService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/attendance")
public class AttendanceApiController {

    private static final Logger log = LoggerFactory.getLogger(AttendanceApiController.class);

    private final AttendanceService attendanceService;
    private final AttendanceMapper attendanceMapper;

    public AttendanceApiController(AttendanceService attendanceService, AttendanceMapper attendanceMapper) {
        this.attendanceService = attendanceService;
        this.attendanceMapper  = attendanceMapper;
    }

    @GetMapping
    public ApiResponse<List<Attendance>> list(
            @RequestParam(defaultValue = "") String date,
            @RequestParam(defaultValue = "daily") String period,
            @AuthenticationPrincipal CrmUserDetails principal) {
        log.info("[Attendance] GET /api/attendance - date={}, period={}", date, period);
        return ApiResponse.ok(attendanceService.findAll(date, period, principal.getGymId()));
    }

    @PostMapping
    public ApiResponse<Attendance> checkIn(@RequestBody Attendance attendance,
                                            @AuthenticationPrincipal CrmUserDetails principal) {
        log.info("[Attendance] POST /api/attendance");
        return ApiResponse.ok(attendanceService.checkIn(attendance, principal.getGymId()));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> cancel(@PathVariable Long id, @AuthenticationPrincipal CrmUserDetails principal) {
        log.info("[Attendance] DELETE /api/attendance/{id} - id={}", id);
        attendanceService.cancel(id, principal.getGymId());
        return ApiResponse.ok();
    }

    @GetMapping("/freeze")
    public ApiResponse<?> frozen(@RequestParam(defaultValue = "") String date,
                                  @AuthenticationPrincipal CrmUserDetails principal) {
        log.info("[Attendance] GET /api/attendance/freeze - date={}", date);
        String d = date.isEmpty() ? LocalDate.now().toString() : date;
        return ApiResponse.ok(attendanceMapper.findFrozen(d, principal.getGymId()));
    }

    @PostMapping("/freeze")
    public ApiResponse<Void> addFreeze(@RequestBody MemberFreeze freeze,
                                        @AuthenticationPrincipal CrmUserDetails principal) {
        log.info("[Attendance] POST /api/attendance/freeze - memberId={}", freeze.getMemberId());
        attendanceMapper.addFreeze(freeze, principal.getGymId());
        return ApiResponse.ok();
    }

    @DeleteMapping("/freeze/{id}")
    public ApiResponse<Void> deleteFreeze(@PathVariable Long id, @AuthenticationPrincipal CrmUserDetails principal) {
        log.info("[Attendance] DELETE /api/attendance/freeze/{id} - id={}", id);
        attendanceMapper.deleteFreeze(id, principal.getGymId());
        return ApiResponse.ok();
    }

    @GetMapping("/trend")
    public ApiResponse<List<Map<String, Object>>> trend(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @AuthenticationPrincipal CrmUserDetails principal) {
        log.info("[Attendance] GET /api/attendance/trend - startDate={}, endDate={}", startDate, endDate);
        String end   = (endDate   != null && !endDate.isEmpty())   ? endDate   : LocalDate.now().toString();
        String start = (startDate != null && !startDate.isEmpty()) ? startDate : LocalDate.now().minusDays(29).toString();
        return ApiResponse.ok(attendanceMapper.dailyTrend(start, end, principal.getGymId()));
    }

    @GetMapping("/member-stats")
    public ApiResponse<List<Map<String, Object>>> memberStats(
            @RequestParam(defaultValue = "") String date,
            @RequestParam(defaultValue = "monthly") String period,
            @AuthenticationPrincipal CrmUserDetails principal) {
        String d = (date == null || date.isBlank()) ? LocalDate.now().toString() : date;
        log.info("[Attendance] GET /api/attendance/member-stats - date={}, period={}", d, period);
        return ApiResponse.ok(attendanceMapper.memberPeriodStats(d, period, principal.getGymId()));
    }

    @GetMapping("/inactive")
    public ApiResponse<List<Map<String, Object>>> inactive(
            @RequestParam(defaultValue = "7") int days,
            @AuthenticationPrincipal CrmUserDetails principal) {
        log.info("[Attendance] GET /api/attendance/inactive - days={}", days);
        return ApiResponse.ok(attendanceMapper.inactiveMembers(days, principal.getGymId()));
    }
}
