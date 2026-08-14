package com.linkfit.admin.controller.api;

import com.linkfit.admin.common.ApiResponse;
import com.linkfit.admin.domain.PtMember;
import com.linkfit.admin.mapper.MemberMapper;
import com.linkfit.admin.security.CrmUserDetails;
import com.linkfit.admin.service.MemberService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/pt")
public class PtApiController {

    private static final Logger log = LoggerFactory.getLogger(PtApiController.class);

    private final MemberMapper memberMapper;
    private final MemberService memberService;

    public PtApiController(MemberMapper memberMapper, MemberService memberService) {
        this.memberMapper = memberMapper;
        this.memberService = memberService;
    }

    @GetMapping("/members")
    public ApiResponse<Map<String, Object>> listPtMembers(
            @RequestParam(defaultValue = "false") boolean lowStock,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("[Pt] GET /api/pt/members - lowStock={}, page={}", lowStock, page);
        int offset = page * size;
        List<PtMember> items = memberMapper.findPtMembers(lowStock, offset, size);
        long total = memberMapper.countPtMembers(lowStock);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("items", items);
        data.put("total", total);
        return ApiResponse.ok(data);
    }

    // 구매 PT(pt_sessions_left)와 서비스 PT(service_pt_sessions_left)를 분리 조정한다.
    // 같은 API를 회원상세(members.html)와 PT 관리(pt.html) 양쪽에서 호출한다.
    @PutMapping("/members/{memberId}/sessions")
    public ApiResponse<Void> adjustSessions(@PathVariable String memberId,
                                             @RequestBody Map<String, Object> body) {
        log.info("[Pt] PUT /api/pt/members/{memberId}/sessions - memberId={}, body={}", memberId, body);
        String target = (String) body.get("target");
        Object rawDelta = body.get("delta");
        if (!(rawDelta instanceof Number)) {
            return ApiResponse.error("delta는 숫자여야 합니다.");
        }
        int delta = ((Number) rawDelta).intValue();
        if (delta == 0) {
            return ApiResponse.error("조정할 횟수를 입력해주세요.");
        }
        if ("SERVICE".equals(target)) {
            memberMapper.adjustServicePtSessions(memberId, delta);
        } else if ("PURCHASED".equals(target)) {
            memberMapper.adjustPtSessions(memberId, delta);
        } else {
            return ApiResponse.error("알 수 없는 대상입니다.");
        }
        return ApiResponse.ok();
    }

    // PT는 특정 구매건 단위가 아니라 회원 전체 PT 잔여 풀(구매+서비스)을 그대로 이전한다.
    @PostMapping("/members/{memberId}/transfer")
    public ApiResponse<Void> transferPt(@PathVariable String memberId, @RequestBody Map<String, String> body,
                                         @AuthenticationPrincipal CrmUserDetails principal) {
        log.info("[Pt] POST /api/pt/members/{memberId}/transfer - memberId={}, body={}", memberId, body);
        String targetMemberId = body.get("targetMemberId");
        if (targetMemberId == null || targetMemberId.isBlank()) {
            return ApiResponse.error("이전받을 회원을 선택해주세요.");
        }
        if (memberId.equals(targetMemberId)) {
            return ApiResponse.error("본인에게는 이전할 수 없습니다.");
        }
        Long gymId = principal.getGymId();
        var source = memberMapper.findById(memberId, gymId).orElse(null);
        if (source == null) {
            return ApiResponse.error("회원을 찾을 수 없습니다.");
        }
        if (!memberMapper.existsInGym(targetMemberId, gymId)) {
            return ApiResponse.error("대상 회원을 찾을 수 없습니다.");
        }
        int purchased = source.getPtSessionsLeft() != null ? source.getPtSessionsLeft() : 0;
        int service = source.getServicePtSessionsLeft() != null ? source.getServicePtSessionsLeft() : 0;
        if (purchased + service <= 0) {
            return ApiResponse.error("이전할 PT 잔여 세션이 없습니다.");
        }
        memberService.transferPtSessions(memberId, targetMemberId, gymId);
        return ApiResponse.ok();
    }
}
