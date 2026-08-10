package com.linkfit.admin.controller.api;

import com.linkfit.admin.common.ApiResponse;
import com.linkfit.admin.domain.CrmMemberNote;
import com.linkfit.admin.domain.CrmMemberTag;
import com.linkfit.admin.domain.Member;
import com.linkfit.admin.domain.MemberTicket;
import com.linkfit.admin.domain.TicketLog;
import com.linkfit.admin.security.CrmUserDetails;
import com.linkfit.admin.service.CrmMemberService;
import com.linkfit.admin.service.MemberService;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/members")
public class MemberApiController {

    private static final Logger log = LoggerFactory.getLogger(MemberApiController.class);

    private final MemberService memberService;
    private final CrmMemberService crmMemberService;

    public MemberApiController(MemberService memberService, CrmMemberService crmMemberService) {
        this.memberService    = memberService;
        this.crmMemberService = crmMemberService;
    }

    @GetMapping
    public ApiResponse<Map<String, Object>> list(
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(defaultValue = "") String status,
            @RequestParam(defaultValue = "") String tier,
            @RequestParam(required = false) List<String> trainerIds,
            @RequestParam(required = false) Integer minDaysLeft,
            @RequestParam(required = false) Integer maxDaysLeft,
            @RequestParam(required = false) Integer minPtRemaining,
            @RequestParam(required = false) Integer minAbsentDays,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal CrmUserDetails principal) {
        log.info("[Member] GET /api/members - keyword={}, status={}, trainerIds={}", keyword, status, trainerIds);
        Long gymId = principal.getGymId();
        List<Member> members = memberService.findAll(keyword, status, tier, gymId, trainerIds,
                minDaysLeft, maxDaysLeft, minPtRemaining, minAbsentDays, page, size);
        long total = memberService.count(keyword, status, tier, gymId, trainerIds,
                minDaysLeft, maxDaysLeft, minPtRemaining, minAbsentDays);
        return ApiResponse.ok(Map.of("members", members, "total", total, "page", page, "size", size));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Member>> get(@PathVariable String id,
                                                    @AuthenticationPrincipal CrmUserDetails principal) {
        log.info("[Member] GET /api/members/{id} - id={}", id);
        return memberService.findById(id, principal.getGymId())
            .map(m -> ResponseEntity.ok(ApiResponse.ok(m)))
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ApiResponse<Member> create(@RequestBody Member member,
                                       @AuthenticationPrincipal CrmUserDetails principal) {
        log.info("[Member] POST /api/members");
        if (member.getName() == null || member.getName().isBlank()
                || member.getPhone() == null || member.getPhone().isBlank()) {
            return ApiResponse.error("이름과 전화번호를 모두 입력해주세요.");
        }
        if (memberService.existsByNameAndPhone(member.getName(), member.getPhone())) {
            return ApiResponse.error("동일한 이름과 전화번호의 회원이 이미 존재합니다.");
        }
        return ApiResponse.ok(memberService.save(member, principal.getGymId()));
    }

    @PutMapping("/{id}")
    public ApiResponse<Member> update(@PathVariable String id, @RequestBody Member member,
                                       @AuthenticationPrincipal CrmUserDetails principal) {
        log.info("[Member] PUT /api/members/{id} - id={}", id);
        return ApiResponse.ok(memberService.update(id, member, principal.getGymId()));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable String id,
                                     @AuthenticationPrincipal CrmUserDetails principal) {
        log.info("[Member] DELETE /api/members/{id} - id={}", id);
        memberService.delete(id, principal.getGymId());
        return ApiResponse.ok();
    }

    @PatchMapping("/{id}/status")
    public ApiResponse<Void> updateStatus(@PathVariable String id, @RequestBody Map<String, String> body,
                                           @AuthenticationPrincipal CrmUserDetails principal) {
        log.info("[Member] PATCH /api/members/{id}/status - id={}", id);
        memberService.updateStatus(id, body.get("status"), principal.getGymId());
        return ApiResponse.ok();
    }

    @PatchMapping("/{id}/tier")
    public ApiResponse<Void> updateTier(@PathVariable String id, @RequestBody Map<String, String> body,
                                         @AuthenticationPrincipal CrmUserDetails principal) {
        log.info("[Member] PATCH /api/members/{id}/tier - id={}", id);
        memberService.updateTier(id, body.get("tier"), principal.getGymId());
        return ApiResponse.ok();
    }

    @PatchMapping("/{id}/member-type")
    public ApiResponse<Void> updateMemberType(@PathVariable String id, @RequestBody Map<String, String> body,
                                               @AuthenticationPrincipal CrmUserDetails principal) {
        log.info("[Member] PATCH /api/members/{id}/member-type - id={}", id);
        memberService.updateMemberType(id, body.get("memberType"), principal.getGymId());
        return ApiResponse.ok();
    }

    @PatchMapping("/{id}/role")
    public ApiResponse<Void> updateRole(@PathVariable String id, @RequestBody Map<String, String> body,
                                         @AuthenticationPrincipal CrmUserDetails principal) {
        log.info("[Member] PATCH /api/members/{id}/role - id={}, role={}", id, body.get("role"));
        memberService.updateRole(id, body.get("role"), principal.getGymId());
        return ApiResponse.ok();
    }

    @PostMapping("/{id}/freeze")
    public ApiResponse<Void> freeze(@PathVariable String id, @RequestBody Map<String, String> body,
                                     @AuthenticationPrincipal CrmUserDetails principal) {
        log.info("[Member] POST /api/members/{id}/freeze - id={}", id);
        memberService.freeze(id, body.get("startDate"), body.get("endDate"), principal.getGymId());
        return ApiResponse.ok();
    }

    @PatchMapping("/{id}/withdraw")
    public ApiResponse<Void> withdraw(@PathVariable String id,
                                       @AuthenticationPrincipal CrmUserDetails principal) {
        log.info("[Member] PATCH /api/members/{id}/withdraw - id={}", id);
        memberService.withdraw(id, principal.getGymId());
        return ApiResponse.ok();
    }

    // 담당 트레이너 지정 — user_profiles.trainer_id (앱이 실제로 사용하는 필드).
    // PUT /{id}/trainer(아래, crm_member_assignments 대상)와는 별개의 값이니 혼동 주의.
    @PatchMapping("/{id}/assigned-trainer")
    public ApiResponse<Void> updateAssignedTrainer(
            @PathVariable String id,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal CrmUserDetails principal) {
        log.info("[Member] PATCH /api/members/{id}/assigned-trainer - id={}", id);
        memberService.updateAssignedTrainer(id, body.get("trainerId"), principal.getGymId());
        return ApiResponse.ok();
    }

    @GetMapping("/{id}/memberships")
    public ApiResponse<List<com.linkfit.admin.domain.Membership>> getMemberships(@PathVariable String id) {
        log.info("[Member] GET /api/members/{id}/memberships - id={}", id);
        return ApiResponse.ok(memberService.findMemberships(id));
    }

    @PostMapping("/{id}/memberships")
    public ApiResponse<Void> addMembership(@PathVariable String id,
                                           @RequestBody com.linkfit.admin.domain.Membership membership,
                                           @AuthenticationPrincipal CrmUserDetails principal) {
        log.info("[Member] POST /api/members/{id}/memberships - id={}", id);
        membership.setMemberId(id);
        memberService.addMembership(membership, principal.getGymId());
        return ApiResponse.ok();
    }

    @GetMapping("/{id}/tickets")
    public ApiResponse<List<MemberTicket>> getTickets(@PathVariable String id) {
        log.info("[Member] GET /api/members/{id}/tickets - id={}", id);
        return ApiResponse.ok(memberService.findTickets(id));
    }

    // 활성 회원 전체의 ONE_POINT/FEEDBACK 잔량 합계 (구독권/티켓 관리 페이지 상단 요약 카드용)
    @GetMapping("/tickets/totals")
    public ApiResponse<Map<String, Object>> ticketTotals(@AuthenticationPrincipal CrmUserDetails principal) {
        log.info("[Member] GET /api/members/tickets/totals");
        return ApiResponse.ok(memberService.ticketTotals(principal.getGymId()));
    }

    @PostMapping("/{id}/tickets/charge")
    public ApiResponse<Void> chargeTicket(@PathVariable String id, @RequestBody Map<String, Object> body) {
        log.info("[Member] POST /api/members/{id}/tickets/charge - id={}", id);
        String ticketType  = (String) body.get("ticketType");
        int amount         = (Integer) body.get("amount");
        String description = (String) body.get("description");
        memberService.chargeTicket(id, ticketType, amount, description);
        return ApiResponse.ok();
    }

    // 티켓 지급/차감 사용내역 (구독권/티켓 관리 > 사용내역 탭)
    @GetMapping("/tickets/logs")
    public ApiResponse<Map<String, Object>> ticketLogs(
            @RequestParam(defaultValue = "") String ticketType,
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal CrmUserDetails principal) {
        log.info("[Member] GET /api/members/tickets/logs - ticketType={}, keyword={}", ticketType, keyword);
        Long gymId = principal.getGymId();
        List<TicketLog> logs = memberService.findTicketLogs(gymId, ticketType, keyword, page, size);
        long total = memberService.countTicketLogs(gymId, ticketType, keyword);
        return ApiResponse.ok(Map.of("logs", logs, "total", total));
    }

    // ── CRM Sector 2 ──────────────────────────────────────────

    @GetMapping("/{id}/notes")
    public ApiResponse<List<CrmMemberNote>> getNotes(
            @PathVariable String id,
            @AuthenticationPrincipal CrmUserDetails principal) {
        log.info("[Member] GET /api/members/{id}/notes - id={}", id);
        return ApiResponse.ok(crmMemberService.findNotes(id, principal.getGymId()));
    }

    @PostMapping("/{id}/notes")
    public ApiResponse<CrmMemberNote> addNote(
            @PathVariable String id,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal CrmUserDetails principal) {
        log.info("[Member] POST /api/members/{id}/notes - id={}", id);
        return ApiResponse.ok(crmMemberService.addNote(
                id, principal.getGymId(), principal.getId(), body.get("content")));
    }

    @GetMapping("/{id}/tags")
    public ApiResponse<List<CrmMemberTag>> getTags(
            @PathVariable String id,
            @AuthenticationPrincipal CrmUserDetails principal) {
        log.info("[Member] GET /api/members/{id}/tags - id={}", id);
        return ApiResponse.ok(crmMemberService.findTags(id, principal.getGymId()));
    }

    @PostMapping("/{id}/tags")
    public ApiResponse<CrmMemberTag> addTag(
            @PathVariable String id,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal CrmUserDetails principal) {
        log.info("[Member] POST /api/members/{id}/tags - id={}", id);
        return ApiResponse.ok(crmMemberService.addTag(
                id, principal.getGymId(), body.get("tag"), body.get("color")));
    }

    @DeleteMapping("/{id}/tags/{tagId}")
    public ApiResponse<Void> deleteTag(
            @PathVariable String id,
            @PathVariable String tagId,
            @AuthenticationPrincipal CrmUserDetails principal) {
        log.info("[Member] DELETE /api/members/{id}/tags/{tagId} - id={}, tagId={}", id, tagId);
        crmMemberService.deleteTag(tagId, principal.getGymId());
        return ApiResponse.ok();
    }

    @GetMapping("/export")
    public void export(
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(defaultValue = "") String status,
            @RequestParam(defaultValue = "") String tier,
            @RequestParam(required = false) List<String> trainerIds,
            HttpServletResponse response,
            @AuthenticationPrincipal CrmUserDetails principal) throws IOException {
        log.info("[Member] GET /api/members/export - keyword={}, status={}", keyword, status);
        List<Member> members = memberService.findAll(keyword, status, tier, principal.getGymId(), trainerIds,
                null, null, null, null, 0, 100_000);

        String filename = "members-" + LocalDate.now() + ".xlsx";
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");

        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("회원 목록");
            String[] headers = { "회원ID", "이름", "이메일", "연락처", "성별", "생년월일", "상태", "가입일", "회원권 만료일", "회원유형", "등급" };
            Row hRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) hRow.createCell(i).setCellValue(headers[i]);
            int rowIdx = 1;
            for (Member m : members) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(m.getId()           != null ? m.getId()                   : "");
                row.createCell(1).setCellValue(m.getName()         != null ? m.getName()                 : "");
                row.createCell(2).setCellValue(m.getEmail()        != null ? m.getEmail()                : "");
                row.createCell(3).setCellValue(m.getPhone()        != null ? m.getPhone()                : "");
                row.createCell(4).setCellValue(m.getGender()       != null ? m.getGender()               : "");
                row.createCell(5).setCellValue(m.getBirthDate()    != null ? m.getBirthDate().toString()  : "");
                row.createCell(6).setCellValue(m.getStatus()       != null ? m.getStatus()               : "");
                row.createCell(7).setCellValue(m.getJoinDate()     != null ? m.getJoinDate().toString()   : "");
                row.createCell(8).setCellValue(m.getMembershipEnd() != null ? m.getMembershipEnd().toString() : "");
                row.createCell(9).setCellValue(m.getMemberType()   != null ? m.getMemberType()           : "");
                row.createCell(10).setCellValue(m.getTier()        != null ? m.getTier()                 : "");
            }
            wb.write(response.getOutputStream());
        }
    }

    @PutMapping("/{id}/trainer")
    public ApiResponse<Void> assignTrainer(
            @PathVariable String id,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal CrmUserDetails principal) {
        log.info("[Member] PUT /api/members/{id}/trainer - id={}", id);
        crmMemberService.assignTrainer(id, principal.getGymId(), body.get("trainerId"));
        return ApiResponse.ok();
    }
}
