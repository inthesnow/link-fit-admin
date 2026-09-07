package com.linkfit.admin.controller.api;

import com.linkfit.admin.common.ApiResponse;
import com.linkfit.admin.domain.Member;
import com.linkfit.admin.domain.Staff;
import com.linkfit.admin.domain.StaffAttendance;
import com.linkfit.admin.mapper.ConversationMapper;
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
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/staff")
public class StaffApiController {

    private static final Logger log = LoggerFactory.getLogger(StaffApiController.class);

    private final StaffService staffService;
    private final StaffMapper staffMapper;
    private final StaffAttendanceMapper staffAttendanceMapper;
    private final ConversationMapper conversationMapper;

    public StaffApiController(StaffService staffService, StaffMapper staffMapper,
                              StaffAttendanceMapper staffAttendanceMapper,
                              ConversationMapper conversationMapper) {
        this.staffService            = staffService;
        this.staffMapper             = staffMapper;
        this.staffAttendanceMapper   = staffAttendanceMapper;
        this.conversationMapper      = conversationMapper;
    }

    @GetMapping
    public ApiResponse<Map<String, Object>> list(
            @RequestParam(defaultValue = "") String role,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal CrmUserDetails principal) {
        log.info("[Staff] GET /api/staff - role={}, page={}", role, page);
        Long gymId = principal.getGymId();
        List<Staff> staff = staffService.findAll(role, page, size, gymId);
        long total = staffService.count(role, gymId);
        return ApiResponse.ok(Map.of("staff", staff, "total", total));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Staff>> get(@PathVariable String id,
                                                   @AuthenticationPrincipal CrmUserDetails principal) {
        log.info("[Staff] GET /api/staff/{id} - id={}", id);
        return staffService.findById(id, principal.getGymId())
            .map(s -> ResponseEntity.ok(ApiResponse.ok(s)))
            .orElse(ResponseEntity.notFound().build());
    }

    private static final Set<String> WORK_STATUSES = Set.of("ACTIVE", "LEAVE", "RESIGNED");

    // 트레이너 등록 화면의 "회원 검색" 드롭박스 — 이름 일부로 검색해 이 지점에 승인된 회원만 보여준다.
    // 예전엔 이름+전화번호 정확 일치로만 찾았는데, 전화번호 포맷이 조금만 달라도(공백/하이픈 등)
    // "회원이 아닙니다"로 잘못 뜨는 문제가 있었음 — 검색해서 직접 고르는 방식으로 바꿔 그 문제 자체를 없앰.
    @GetMapping("/member-candidates")
    public ApiResponse<List<Staff>> searchMemberCandidates(
            @RequestParam(defaultValue = "") String keyword,
            @AuthenticationPrincipal CrmUserDetails principal) {
        log.info("[Staff] GET /api/staff/member-candidates - keyword={}", keyword);
        String trimmed = keyword.trim();
        if (trimmed.isEmpty()) {
            return ApiResponse.ok(List.of());
        }
        return ApiResponse.ok(staffService.searchMemberCandidates(trimmed, principal.getGymId()));
    }

    // 트레이너 지정은 이 페이지에서만 진행한다. 신규 계정을 만드는 것이 아니라, 위 검색에서
    // 선택한 기존 앱 회원의 role만 TRAINER로 승격시킨다 (기존 이력 보존, 중복 계정 생성 방지).
    @PostMapping
    public ApiResponse<Staff> create(@RequestBody Map<String, String> body,
                                     @AuthenticationPrincipal CrmUserDetails principal) {
        log.info("[Staff] POST /api/staff - memberId={}", body.get("memberId"));
        String memberId = body.get("memberId");
        if (memberId == null || memberId.isBlank()) {
            return ApiResponse.error("트레이너로 지정할 회원을 검색해서 선택해주세요.");
        }
        Long gymId = principal.getGymId();
        Staff matched = staffService.findMemberCandidate(memberId, gymId).orElse(null);
        if (matched == null) {
            return ApiResponse.error("선택한 회원을 찾을 수 없습니다. 이 지점에 가입 승인된 회원만 트레이너로 지정할 수 있습니다.");
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

        String workStatus = body.getOrDefault("workStatus", "ACTIVE");
        if (!WORK_STATUSES.contains(workStatus)) {
            return ApiResponse.error("근무상태 값이 올바르지 않습니다.");
        }
        LocalDate hireDate = parseDateOrNull(body.get("hireDate"));
        LocalDate resignationDate = parseDateOrNull(body.get("resignationDate"));
        if ("RESIGNED".equals(workStatus) && resignationDate == null) {
            return ApiResponse.error("근무상태가 퇴사인 경우 퇴사일을 입력해주세요.");
        }

        return ApiResponse.ok(staffService.promoteToTrainer(matched.getId(), gymId, hireDate, workStatus, resignationDate));
    }

    @PutMapping("/{id}")
    public ApiResponse<Staff> update(@PathVariable String id, @RequestBody Staff staff,
                                     @AuthenticationPrincipal CrmUserDetails principal) {
        log.info("[Staff] PUT /api/staff/{id} - id={}", id);
        String workStatus = staff.getWorkStatus();
        if (workStatus != null && !workStatus.isBlank() && !WORK_STATUSES.contains(workStatus)) {
            return ApiResponse.error("근무상태 값이 올바르지 않습니다.");
        }
        if ("RESIGNED".equals(workStatus) && staff.getResignationDate() == null) {
            return ApiResponse.error("근무상태가 퇴사인 경우 퇴사일을 입력해주세요.");
        }
        return ApiResponse.ok(staffService.update(id, staff, principal.getGymId()));
    }

    private static LocalDate parseDateOrNull(String value) {
        if (value == null || value.isBlank()) return null;
        return LocalDate.parse(value);
    }

    // "삭제"가 아니라 트레이너 권한 회수. 앱 계정은 그대로 두고 role만 MEMBER로 되돌리며,
    // CRM 로그인 계정은 비활성화한다 (계정 자체/이력은 보존).
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable String id,
                                    @AuthenticationPrincipal CrmUserDetails principal) {
        log.info("[Staff] DELETE /api/staff/{id} - id={}", id);
        staffService.revokeTrainer(id, principal.getGymId());
        return ApiResponse.ok();
    }

    @PatchMapping("/{id}/role")
    public ApiResponse<Void> updateRole(@PathVariable String id, @RequestBody Map<String, String> body,
                                        @AuthenticationPrincipal CrmUserDetails principal) {
        log.info("[Staff] PATCH /api/staff/{id}/role - id={}", id);
        staffService.updateRole(id, body.get("role"), principal.getGymId());
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

    // 트레이너 관리영역 "쪽지내역보기" — 회원<->트레이너 쪽지(message_conversation/chat_message)를
    // 관리자가 오버사이트 목적으로 읽기전용 조회. findById로 이 트레이너가 실제 이 지점 소속인지
    // 먼저 확인(message_conversation엔 gym_id가 없어 이 확인이 유일한 스코핑 수단).
    @GetMapping("/{id}/messages")
    public ApiResponse<List<Map<String, Object>>> trainerMessages(
            @PathVariable String id,
            @AuthenticationPrincipal CrmUserDetails principal) {
        log.info("[Staff] GET /api/staff/{id}/messages - id={}", id);
        if (staffMapper.findById(id, principal.getGymId()).isEmpty()) {
            return ApiResponse.error("트레이너를 찾을 수 없습니다.");
        }
        return ApiResponse.ok(conversationMapper.findMessagesByTrainer(id, 100));
    }

    // ── 출근 관리 ──────────────────────────────────────────────

    @GetMapping("/attendance/today")
    public ApiResponse<List<StaffAttendance>> todayStatus(@AuthenticationPrincipal CrmUserDetails principal) {
        log.info("[Staff] GET /api/staff/attendance/today");
        return ApiResponse.ok(staffAttendanceMapper.findTodayStatus(LocalDate.now(), principal.getGymId()));
    }

    @GetMapping("/attendance")
    public ApiResponse<List<StaffAttendance>> history(
            @RequestParam(defaultValue = "") String startDate,
            @RequestParam(defaultValue = "") String endDate,
            @RequestParam(defaultValue = "") String staffId,
            @AuthenticationPrincipal CrmUserDetails principal) {
        LocalDate start = startDate.isEmpty() ? LocalDate.now().withDayOfMonth(1) : LocalDate.parse(startDate);
        LocalDate end   = endDate.isEmpty()   ? LocalDate.now()                   : LocalDate.parse(endDate);
        log.info("[Staff] GET /api/staff/attendance - start={}, end={}, staffId={}", start, end, staffId);
        return ApiResponse.ok(staffAttendanceMapper.findHistory(start, end, staffId.isEmpty() ? null : staffId,
                principal.getGymId()));
    }

    @PostMapping("/attendance/checkin")
    public ApiResponse<StaffAttendance> checkIn(@RequestBody Map<String, String> body,
                                                @AuthenticationPrincipal CrmUserDetails principal) {
        log.info("[Staff] POST /api/staff/attendance/checkin - userId={}", body.get("userId"));
        StaffAttendance sa = new StaffAttendance();
        sa.setUserId(body.get("userId"));
        sa.setAttendDate(body.get("date") != null ? LocalDate.parse(body.get("date")) : LocalDate.now());
        sa.setCheckIn(body.get("time") != null ? LocalTime.parse(body.get("time")) : LocalTime.now().withSecond(0).withNano(0));
        sa.setMemo(body.get("memo"));
        staffAttendanceMapper.insert(sa, principal.getGymId());
        return ApiResponse.ok(sa);
    }

    @PatchMapping("/attendance/{id}/checkout")
    public ApiResponse<Void> checkOut(@PathVariable Long id, @RequestBody(required = false) Map<String, String> body,
                                      @AuthenticationPrincipal CrmUserDetails principal) {
        log.info("[Staff] PATCH /api/staff/attendance/{}/checkout", id);
        LocalTime time = (body != null && body.get("time") != null)
                ? LocalTime.parse(body.get("time"))
                : LocalTime.now().withSecond(0).withNano(0);
        staffAttendanceMapper.updateCheckOut(id, time, principal.getGymId());
        return ApiResponse.ok();
    }

    @DeleteMapping("/attendance/{id}")
    public ApiResponse<Void> deleteAttendance(@PathVariable Long id,
                                              @AuthenticationPrincipal CrmUserDetails principal) {
        log.info("[Staff] DELETE /api/staff/attendance/{}", id);
        staffAttendanceMapper.delete(id, principal.getGymId());
        return ApiResponse.ok();
    }
}
