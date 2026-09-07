package com.linkfit.admin.controller.api;

import com.linkfit.admin.common.ApiResponse;
import com.linkfit.admin.domain.ClassAttendee;
import com.linkfit.admin.domain.ClassSession;
import com.linkfit.admin.domain.OnepointRequest;
import com.linkfit.admin.domain.TrainerSchedule;
import com.linkfit.admin.mapper.ClassMapper;
import com.linkfit.admin.mapper.OnepointRequestMapper;
import com.linkfit.admin.mapper.TrainerScheduleMapper;
import com.linkfit.admin.security.CrmUserDetails;
import com.linkfit.admin.service.ClassService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/classes")
public class ClassApiController {

    private static final Logger log = LoggerFactory.getLogger(ClassApiController.class);

    private final ClassService classService;
    private final ClassMapper classMapper;
    private final TrainerScheduleMapper trainerScheduleMapper;
    private final OnepointRequestMapper onepointRequestMapper;

    public ClassApiController(ClassService classService, ClassMapper classMapper,
                              TrainerScheduleMapper trainerScheduleMapper,
                              OnepointRequestMapper onepointRequestMapper) {
        this.classService            = classService;
        this.classMapper             = classMapper;
        this.trainerScheduleMapper   = trainerScheduleMapper;
        this.onepointRequestMapper   = onepointRequestMapper;
    }

    @GetMapping
    public ApiResponse<Map<String, Object>> list(
            @RequestParam(defaultValue = "") String type,
            @RequestParam(defaultValue = "") String date,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal CrmUserDetails principal) {
        log.info("[Class] GET /api/classes - type={}, date={}", type, date);
        Long gymId = principal.getGymId();
        return ApiResponse.ok(Map.of(
            "classes", classService.findAll(type, date, page, size, gymId),
            "total", classService.count(type, date, gymId)
        ));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ClassSession>> get(@PathVariable Long id,
                                                          @AuthenticationPrincipal CrmUserDetails principal) {
        log.info("[Class] GET /api/classes/{id} - id={}", id);
        return classService.findById(id, principal.getGymId())
            .map(c -> ResponseEntity.ok(ApiResponse.ok(c)))
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ApiResponse<ClassSession> create(@RequestBody ClassSession session,
                                             @AuthenticationPrincipal CrmUserDetails principal) {
        log.info("[Class] POST /api/classes");
        return ApiResponse.ok(classService.save(session, principal.getGymId()));
    }

    @PutMapping("/{id}")
    public ApiResponse<ClassSession> update(@PathVariable Long id, @RequestBody ClassSession session,
                                             @AuthenticationPrincipal CrmUserDetails principal) {
        log.info("[Class] PUT /api/classes/{id} - id={}", id);
        return ApiResponse.ok(classService.update(id, session, principal.getGymId()));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> cancel(@PathVariable Long id, @AuthenticationPrincipal CrmUserDetails principal) {
        log.info("[Class] DELETE /api/classes/{id} - id={}", id);
        classService.cancel(id, principal.getGymId());
        return ApiResponse.ok();
    }

    @GetMapping("/{id}/attendees")
    public ApiResponse<List<ClassAttendee>> getAttendees(@PathVariable Long id,
                                                          @AuthenticationPrincipal CrmUserDetails principal) {
        log.info("[Class] GET /api/classes/{id}/attendees - id={}", id);
        return ApiResponse.ok(classMapper.findAttendees(id, principal.getGymId()));
    }

    @PostMapping("/{id}/attendees")
    public ApiResponse<Void> enroll(@PathVariable Long id, @RequestBody Map<String, String> body,
                                     @AuthenticationPrincipal CrmUserDetails principal) {
        log.info("[Class] POST /api/classes/{id}/attendees - id={}", id);
        classService.enroll(id, body.get("memberId"), principal.getGymId());
        return ApiResponse.ok();
    }

    @DeleteMapping("/{id}/attendees/{memberId}")
    public ApiResponse<Void> cancelEnrollment(@PathVariable Long id, @PathVariable String memberId,
                                               @AuthenticationPrincipal CrmUserDetails principal) {
        log.info("[Class] DELETE /api/classes/{id}/attendees/{memberId} - id={}, memberId={}", id, memberId);
        classService.cancelEnrollment(id, memberId, principal.getGymId());
        return ApiResponse.ok();
    }

    // ── 트레이너 일정 (trainer_schedules) ──────────────────────

    @GetMapping("/schedules")
    public ApiResponse<List<TrainerSchedule>> schedules(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month,
            @AuthenticationPrincipal CrmUserDetails principal) {
        LocalDate ref = LocalDate.now();
        int y = (year  != null) ? year  : ref.getYear();
        int m = (month != null) ? month : ref.getMonthValue();
        log.info("[Class] GET /api/classes/schedules - year={}, month={}", y, m);
        return ApiResponse.ok(trainerScheduleMapper.findByMonth(y, m, principal.getGymId()));
    }

    @GetMapping("/schedules/date")
    public ApiResponse<List<TrainerSchedule>> schedulesByDate(@RequestParam String date,
                                                                @AuthenticationPrincipal CrmUserDetails principal) {
        log.info("[Class] GET /api/classes/schedules/date - date={}", date);
        return ApiResponse.ok(trainerScheduleMapper.findByDate(date, principal.getGymId()));
    }

    // 트레이너 개인 스케줄 (캘린더 대신 목록으로 조회)
    @GetMapping("/schedules/trainer/{trainerId}")
    public ApiResponse<List<TrainerSchedule>> schedulesByTrainer(
            @PathVariable String trainerId,
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate,
            @AuthenticationPrincipal CrmUserDetails principal) {
        log.info("[Class] GET /api/classes/schedules/trainer/{trainerId} - trainerId={}, fromDate={}, toDate={}",
                trainerId, fromDate, toDate);
        return ApiResponse.ok(trainerScheduleMapper.findByTrainer(trainerId, fromDate, toDate, principal.getGymId()));
    }

    // ── 원포인트 신청 (onepoint_requests) ─────────────────────

    @GetMapping("/onepoint/requests")
    public ApiResponse<Map<String, Object>> onepointRequests(
            @RequestParam(defaultValue = "")  String status,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal CrmUserDetails principal) {
        log.info("[Class] GET /api/classes/onepoint/requests - status={}", status);
        Long gymId = principal.getGymId();
        List<OnepointRequest> list = onepointRequestMapper.findAll(status, page * size, size, gymId);
        long total                 = onepointRequestMapper.count(status, gymId);
        return ApiResponse.ok(Map.of("requests", list, "total", total, "page", page));
    }

    @PatchMapping("/onepoint/requests/{id}/status")
    public ApiResponse<Void> updateOnepointStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal CrmUserDetails principal) {
        log.info("[Class] PATCH /api/classes/onepoint/requests/{}/status - status={}", id, body.get("status"));
        onepointRequestMapper.updateStatus(id, body.get("status"), body.get("note"), principal.getGymId());
        return ApiResponse.ok();
    }
}
