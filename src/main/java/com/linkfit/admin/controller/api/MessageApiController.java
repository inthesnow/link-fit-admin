package com.linkfit.admin.controller.api;

import com.linkfit.admin.common.ApiResponse;
import com.linkfit.admin.mapper.ConversationMapper;
import com.linkfit.admin.security.CrmUserDetails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/messages")
public class MessageApiController {

    private static final Logger log = LoggerFactory.getLogger(MessageApiController.class);

    private final ConversationMapper conversationMapper;

    public MessageApiController(ConversationMapper conversationMapper) {
        this.conversationMapper = conversationMapper;
    }

    // ── 발신자(트레이너) 목록 ──────────────────────────────────────────

    @GetMapping("/senders")
    public ApiResponse<List<Map<String, Object>>> senders(@AuthenticationPrincipal CrmUserDetails principal) {
        log.info("[Message] GET /api/messages/senders");
        return ApiResponse.ok(conversationMapper.findSenders(gymId(principal)));
    }

    // ── 발송 대상 인원 수 미리보기 ────────────────────────────────────

    @GetMapping("/preview-count")
    public ApiResponse<Map<String, Object>> previewCount(
            @RequestParam(defaultValue = "all_members") String targetType,
            @RequestParam(defaultValue = "") String tier,
            @RequestParam(defaultValue = "") String trainerUserId,
            @AuthenticationPrincipal CrmUserDetails principal) {
        log.info("[Message] GET /api/messages/preview-count - targetType={}, tier={}", targetType, tier);
        long count = conversationMapper.countTarget(gymId(principal), targetType,
                tier.isEmpty() ? null : tier,
                trainerUserId.isEmpty() ? null : trainerUserId);
        return ApiResponse.ok(Map.of("count", count, "targetType", targetType));
    }

    // ── 발송 이력 ─────────────────────────────────────────────────────

    @GetMapping
    public ApiResponse<Map<String, Object>> list(
            @RequestParam(defaultValue = "") String category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("[Message] GET /api/messages - category={}, page={}", category, page);
        String cat = category.isEmpty() ? null : category;
        List<Map<String, Object>> list = conversationMapper.findBroadcasts(cat, page * size, size);
        long total = conversationMapper.countBroadcasts(cat);
        return ApiResponse.ok(Map.of("messages", list, "total", total, "page", page));
    }

    // ── 메시지 발송 ───────────────────────────────────────────────────

    /**
     * 공지·이벤트 일괄 발송
     * body: { category, targetType, tier, trainerUserId, senderUserId, content }
     * - targetType: all_members | all_trainers | tier | trainer
     * - category: 공지 | 이벤트
     */
    @PostMapping("/broadcast")
    public ApiResponse<Map<String, Object>> broadcast(@RequestBody Map<String, Object> body,
                                                        @AuthenticationPrincipal CrmUserDetails principal) {
        Long gymId = gymId(principal);
        String category    = (String) body.getOrDefault("category",    "공지");
        String targetType  = (String) body.getOrDefault("targetType",  "all_members");
        String tier        = (String) body.getOrDefault("tier",        "");
        String trainerUserId = (String) body.getOrDefault("trainerUserId", "");
        String senderUserId = (String) body.getOrDefault("senderUserId", "");
        String content     = (String) body.getOrDefault("content",     "");

        log.info("[Message] POST /api/messages/broadcast - category={}, targetType={}, tier={}", category, targetType, tier);

        if (senderUserId.isEmpty()) {
            return ApiResponse.error("발신 트레이너를 선택해주세요.");
        }
        if (content.isBlank()) {
            return ApiResponse.error("메시지 내용을 입력해주세요.");
        }

        List<String> targetIds;
        boolean toTrainers = "all_trainers".equals(targetType);

        if (toTrainers) {
            targetIds = conversationMapper.findTrainerTargets(gymId);
        } else {
            String tierFilter   = (tier == null || tier.isEmpty())         ? null : tier;
            String trainerFilter = (trainerUserId == null || trainerUserId.isEmpty()) ? null : trainerUserId;
            targetIds = conversationMapper.findMemberTargets(gymId, tierFilter, trainerFilter);
        }

        int sent  = 0;
        List<String> failures = new ArrayList<>();

        for (String recipientId : targetIds) {
            try {
                Map<String, Object> conv = new HashMap<>();
                if (toTrainers) {
                    // 트레이너 수신: trainer_id=recipient, member_id=sender(트레이너)
                    conv.put("memberId",  senderUserId);
                    conv.put("trainerId", recipientId);
                } else {
                    // 회원 수신: member_id=recipient, trainer_id=sender(트레이너)
                    conv.put("memberId",  recipientId);
                    conv.put("trainerId", senderUserId);
                }
                conv.put("category", category);
                conversationMapper.insertConversation(conv);

                long convId = ((Number) conv.get("id")).longValue();
                conversationMapper.insertChatMessage(convId, senderUserId, content);
                sent++;
            } catch (Exception e) {
                log.warn("[Message] broadcast insert failed for recipient={}: {}", recipientId, e.getMessage());
                failures.add(recipientId);
            }
        }

        log.info("[Message] broadcast complete - sent={}, failed={}", sent, failures.size());
        return ApiResponse.ok(Map.of("sent", sent, "failed", failures.size(), "total", targetIds.size()));
    }

    private Long gymId(CrmUserDetails principal) {
        return principal != null ? principal.getGymId() : 1L;
    }
}
