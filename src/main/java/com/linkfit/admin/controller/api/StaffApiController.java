package com.linkfit.admin.controller.api;

import com.linkfit.admin.common.ApiResponse;
import com.linkfit.admin.domain.Member;
import com.linkfit.admin.domain.Staff;
import com.linkfit.admin.domain.StaffAttendance;
import com.linkfit.admin.mapper.StaffAttendanceMapper;
import com.linkfit.admin.mapper.StaffMapper;
import com.linkfit.admin.security.CrmUserDetails;
import com.linkfit.admin.service.StaffService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/staff")
public class StaffApiController {

    private static final Logger log = LoggerFactory.getLogger(StaffApiController.class);

    private final StaffService staffService;
    private final StaffMapper staffMapper;
    private final StaffAttendanceMapper staffAttendanceMapper;

    public StaffApiController(StaffService staffService, StaffMapper staffMapper,
                              StaffAttendanceMapper staffAttendanceMapper) {
        this.staffService            = staffService;
        this.staffMapper             = staffMapper;
        this.staffAttendanceMapper   = staffAttendanceMapper;
    }

    @GetMapping
    public ApiResponse<Map<String, Object>> list(
            @RequestParam(defaultValue = "") String role,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        log.info("[Staff] GET /api/staff - role={}, page={}", role, page);
        List<Staff> staff = staffService.findAll(role, page, size);
        long total = staffService.count(role);
        return ApiResponse.ok(Map.of("staff", staff, "total", total));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Staff>> get(@PathVariable String id) {
        log.info("[Staff] GET /api/staff/{id} - id={}", id);
        return staffService.findById(id)
            .map(s -> ResponseEntity.ok(ApiResponse.ok(s)))
            .orElse(ResponseEntity.notFound().build());
    }

    // 트레이너 지정은 이 페이지에서만 진행한다. 신규 계정을 만드는 것이 아니라, 이름+전화번호로
    // 이미 앱에 가입된 사용자를 찾아 그 계정의 role만 TRAINER로 승격시킨다 (기존 이력 보존,
    // 중복 계정 생성 방지). 일치하는 앱 사용자가 없으면 등록을 거부한다.
    @PostMapping
    public ApiResponse<Staff> create(@RequestBody Staff staff) {
        log.info("[Staff] POST /api/staff - name={}", staff.getName());
        String name = staff.getName() == null ? "" : staff.getName().trim();
        String phone = staff.getPhone() == null ? "" : staff.getPhone().replaceAll("[^0-9]", "");
        if (name.isEmpty() || phone.isEmpty()) {
            return ApiResponse.error("이름과 전화번호를 입력해주세요.");
        }
        Staff matched = staffService.findAppUserByNameAndPhone(name, phone).orElse(null);
        if (matched == null) {
            return ApiResponse.error("해당 이름/전화번호로 가입된 앱 사용자를 찾을 수 없습니다. 먼저 앱에 회원가입이 되어 있어야 트레이너로 지정할 수 있습니다.");
        }
        if ("TRAINER".equals(matched.getRole())) {
            return ApiResponse.error("이미 트레이너로 등록되어 있는 사용자입니다.");
        }
        if ("ADMIN".equals(matched.getRole())) {
            return ApiResponse.error("관리자 계정은 트레이너로 전환할 수 없습니다.");
        }
        // 앱 가입 시 헬스장을 선택하지 않은 사용자는 트레이너로 지정할 수 없다 (소속 없는
        // 트레이너를 만들지 않기 위함 — 회원 쪽 트레이너 노출 필터링이 소속 매칭 기반이라
        // 소속 없는 트레이너가 생기면 그 필터링 자체가 무의미해진다).
        if (staffService.findTrainerGymId(matched.getId()) == null) {
            return ApiResponse.error("이 사용자는 앱에서 소속 헬스장을 선택하지 않았습니다. 앱에서 헬스장을 먼저 선택해야 트레이너로 지정할 수 있습니다.");
        }
        return ApiResponse.ok(staffService.promoteToTrainer(matched.getId()));
    }

    @PutMapping("/{id}")
    public ApiResponse<Staff> update(@PathVariable String id, @RequestBody Staff staff) {
        log.info("[Staff] PUT /api/staff/{id} - id={}", id);
        return ApiResponse.ok(staffService.update(id, staff));
    }

    // "삭제"가 아니라 트레이너 권한 회수. 앱 계정은 그대로 두고 role만 MEMBER로 되돌리며,
    // CRM 로그인 계정은 비활성화한다 (계정 자체/이력은 보존).
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable String id) {
        log.info("[Staff] DELETE /api/staff/{id} - id={}", id);
        staffService.revokeTrainer(id);
        return ApiResponse.ok();
    }

    @PatchMapping("/{id}/role")
    public ApiResponse<Void> updateRole(@PathVariable String id, @RequestBody Map<String, String> body) {
        log.info("[Staff] PATCH /api/staff/{id}/role - id={}", id);
        staffService.updateRole(id, body.get("role"));
        return ApiResponse.ok();
    }

    // ── Sector 13: 트레이너 CRM 대시보드 ──────────────────────

    @GetMapping("/{id}/dashboard")
    public ApiResponse<Map<String, Object>> dashboard(
            @PathVariable String id,
            @AuthenticationPrincipal CrmUserDetails principal) {
        log.info("[Staff] GET /api/staff/{id}/dashboard - id={}", id);
        Map<String, Object> stats = staffMapper.findDashboard(id, principal.getGymId());
        return ApiResponse.ok(stats != null ? stats : Map.of(
                "assignedMembers", 0, "pendingFeedback", 0, "pendingReregistration", 0));
    }

    @GetMapping("/{id}/members")
    public ApiResponse<List<Member>> assignedMembers(
            @PathVariable String id,
            @AuthenticationPrincipal CrmUserDetails principal) {
        log.info("[Staff] GET /api/staff/{id}/members - id={}", id);
        return ApiResponse.ok(staffMapper.findAssignedMembers(id, principal.getGymId()));
    }

    // ── 출근 관리 ──────────────────────────────────────────────

    @GetMapping("/attendance/today")
    public ApiResponse<List<StaffAttendance>> todayStatus() {
        log.info("[Staff] GET /api/staff/attendance/today");
        return ApiResponse.ok(staffAttendanceMapper.findTodayStatus(LocalDate.now()));
    }

    @GetMapping("/attendance")
    public ApiResponse<List<StaffAttendance>> history(
            @RequestParam(defaultValue = "") String startDate,
            @RequestParam(defaultValue = "") String endDate,
            @RequestParam(defaultValue = "") String staffId) {
        LocalDate start = startDate.isEmpty() ? LocalDate.now().withDayOfMonth(1) : LocalDate.parse(startDate);
        LocalDate end   = endDate.isEmpty()   ? LocalDate.now()                   : LocalDate.parse(endDate);
        log.info("[Staff] GET /api/staff/attendance - start={}, end={}, staffId={}", start, end, staffId);
        return ApiResponse.ok(staffAttendanceMapper.findHistory(start, end, staffId.isEmpty() ? null : staffId));
    }

    @PostMapping("/attendance/checkin")
    public ApiResponse<StaffAttendance> checkIn(@RequestBody Map<String, String> body) {
        log.info("[Staff] POST /api/staff/attendance/checkin - userId={}", body.get("userId"));
        StaffAttendance sa = new StaffAttendance();
        sa.setUserId(body.get("userId"));
        sa.setAttendDate(body.get("date") != null ? LocalDate.parse(body.get("date")) : LocalDate.now());
        sa.setCheckIn(body.get("time") != null ? LocalTime.parse(body.get("time")) : LocalTime.now().withSecond(0).withNano(0));
        sa.setMemo(body.get("memo"));
        staffAttendanceMapper.insert(sa);
        return ApiResponse.ok(sa);
    }

    @PatchMapping("/attendance/{id}/checkout")
    public ApiResponse<Void> checkOut(@PathVariable Long id, @RequestBody(required = false) Map<String, String> body) {
        log.info("[Staff] PATCH /api/staff/attendance/{}/checkout", id);
        LocalTime time = (body != null && body.get("time") != null)
                ? LocalTime.parse(body.get("time"))
                : LocalTime.now().withSecond(0).withNano(0);
        staffAttendanceMapper.updateCheckOut(id, time);
        return ApiResponse.ok();
    }

    @DeleteMapping("/attendance/{id}")
    public ApiResponse<Void> deleteAttendance(@PathVariable Long id) {
        log.info("[Staff] DELETE /api/staff/attendance/{}", id);
        staffAttendanceMapper.delete(id);
        return ApiResponse.ok();
    }
}
