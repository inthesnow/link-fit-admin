package com.linkfit.admin.controller.api;

import com.linkfit.admin.common.ApiResponse;
import com.linkfit.admin.domain.CrmMembershipHistory;
import com.linkfit.admin.domain.Membership;
import com.linkfit.admin.mapper.MemberMapper;

import com.linkfit.admin.security.CrmUserDetails;
import com.linkfit.admin.service.CrmMemberService;
import com.linkfit.admin.service.MemberService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/memberships")
public class MembershipApiController {

    private static final Logger log = LoggerFactory.getLogger(MembershipApiController.class);

    private final MemberMapper memberMapper;
    private final CrmMemberService crmMemberService;
    private final MemberService memberService;

    public MembershipApiController(MemberMapper memberMapper, CrmMemberService crmMemberService, MemberService memberService) {
        this.memberMapper     = memberMapper;
        this.crmMemberService = crmMemberService;
        this.memberService    = memberService;
    }

    // 이용권/락커/운동복 양도 (PT는 /api/pt/members/{id}/transfer 사용).
    // 검증은 여기서 다 하고 ApiResponse.error로 구체적 메시지를 내려준다 — 서비스 계층에서
    // IllegalArgumentException을 던지면 GlobalExceptionHandler가 전부 "서버 오류가 발생했습니다"로
    // 뭉개버려서 사용자에게 원인이 안 보이기 때문.
    @PostMapping("/{id}/transfer")
    public ApiResponse<Void> transfer(@PathVariable Long id, @RequestBody Map<String, String> body,
                                       @AuthenticationPrincipal CrmUserDetails principal) {
        log.info("[Membership] POST /api/memberships/{}/transfer - body={}", id, body);
        String targetMemberId = body.get("targetMemberId");
        if (targetMemberId == null || targetMemberId.isBlank()) {
            return ApiResponse.error("양도받을 회원을 선택해주세요.");
        }
        Membership target = memberMapper.findMembershipById(id, principal.getGymId()).orElse(null);
        if (target == null) {
            return ApiResponse.error("이용권을 찾을 수 없습니다.");
        }
        if ("PT".equals(target.getType())) {
            return ApiResponse.error("PT는 개별 양도 대상이 아닙니다. PT 잔여 이전 기능을 이용해주세요.");
        }
        if (target.getStatus() != null) {
            return ApiResponse.error("이미 양도되었거나 더 이상 유효하지 않은 이용권입니다.");
        }
        if (target.getMemberId().equals(targetMemberId)) {
            return ApiResponse.error("본인에게는 양도할 수 없습니다.");
        }
        if (!memberMapper.existsInGym(targetMemberId, principal.getGymId())) {
            return ApiResponse.error("대상 회원을 찾을 수 없습니다.");
        }
        memberService.transferMembership(id, targetMemberId, principal.getGymId());
        return ApiResponse.ok();
    }

    @GetMapping("/expiring")
    public ApiResponse<Map<String, Object>> expiring(
            @RequestParam(defaultValue = "30") int days,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal CrmUserDetails principal) {
        log.info("[Membership] GET /api/memberships/expiring - days={}, page={}", days, page);
        Long gymId = principal.getGymId();
        List<Membership> list  = memberMapper.findExpiringMemberships(days, gymId, page * size, size);
        long total             = memberMapper.countExpiringMemberships(days, gymId);
        return ApiResponse.ok(Map.of("memberships", list, "total", total, "page", page, "size", size, "days", days));
    }

    @GetMapping("/member/{memberId}/history")
    public ApiResponse<List<CrmMembershipHistory>> getHistory(
            @PathVariable String memberId,
            @AuthenticationPrincipal CrmUserDetails principal) {
        log.info("[Membership] GET /api/memberships/member/{memberId}/history - memberId={}", memberId);
        return ApiResponse.ok(crmMemberService.findMembershipHistory(memberId, principal.getGymId()));
    }

    @PatchMapping("/{id}/end-date")
    public ApiResponse<Void> updateEndDate(@PathVariable Long id, @RequestBody Map<String, String> body,
                                            @AuthenticationPrincipal CrmUserDetails principal) {
        log.info("[Membership] PATCH /api/memberships/{}/end-date", id);
        memberMapper.updateMembershipEndDate(id, body.get("endDate"), principal.getGymId());
        return ApiResponse.ok();
    }

    @PatchMapping("/{id}/adjust-period")
    public ApiResponse<Map<String, String>> adjustPeriod(@PathVariable Long id, @RequestBody Map<String, Integer> body,
                                                           @AuthenticationPrincipal CrmUserDetails principal) {
        log.info("[Membership] PATCH /api/memberships/{}/adjust-period - body={}", id, body);
        Membership ms = memberMapper.findMembershipById(id, principal.getGymId())
                .orElseThrow(() -> new IllegalArgumentException("이용권을 찾을 수 없습니다."));
        int months = body.getOrDefault("months", 0);
        int days   = body.getOrDefault("days", 0);
        java.time.LocalDate newEndDate = ms.getEndDate().plusMonths(months).plusDays(days);
        memberMapper.updateMembershipEndDate(id, newEndDate.toString(), principal.getGymId());
        return ApiResponse.ok(Map.of("endDate", newEndDate.toString()));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id, @AuthenticationPrincipal CrmUserDetails principal) {
        log.info("[Membership] DELETE /api/memberships/{}", id);
        memberService.deleteMembership(id, principal.getGymId());
        return ApiResponse.ok();
    }

    @DeleteMapping("/member/{memberId}/package/{packageId}")
    public ApiResponse<Void> deleteByPackage(@PathVariable String memberId, @PathVariable Long packageId,
                                              @AuthenticationPrincipal CrmUserDetails principal) {
        log.info("[Membership] DELETE all by package - memberId={}, packageId={}", memberId, packageId);
        memberService.deleteMembershipsByPackage(memberId, packageId, principal.getGymId());
        return ApiResponse.ok();
    }

    @PostMapping("/member/{memberId}/actions")
    public ApiResponse<Void> recordAction(
            @PathVariable String memberId,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal CrmUserDetails principal) {
        log.info("[Membership] POST /api/memberships/member/{memberId}/actions - memberId={}", memberId);
        crmMemberService.recordMembershipAction(
                memberId, principal.getGymId(),
                body.get("action"), body.get("reason"),
                principal.getId());
        return ApiResponse.ok();
    }
}
