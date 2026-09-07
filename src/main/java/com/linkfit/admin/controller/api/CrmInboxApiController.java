package com.linkfit.admin.controller.api;

import com.linkfit.admin.common.ApiResponse;
import com.linkfit.admin.domain.CrmMessage;
import com.linkfit.admin.mapper.CrmMessageMapper;
import com.linkfit.admin.security.CrmUserDetails;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/inbox")
public class CrmInboxApiController {

    private static final Logger log = LoggerFactory.getLogger(CrmInboxApiController.class);

    private final CrmMessageMapper messageMapper;

    public CrmInboxApiController(CrmMessageMapper messageMapper) {
        this.messageMapper = messageMapper;
    }

    @GetMapping("/messages")
    public ApiResponse<Map<String, Object>> list(
            @RequestParam(defaultValue = "received") String type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal CrmUserDetails principal) {
        log.info("[CrmInbox] GET /api/inbox/messages - type={}, page={}", type, page);
        Long gymId = (principal != null) ? principal.getGymId() : 1L;
        String userId = (principal != null) ? principal.getId() : "unknown";
        int offset = page * size;

        List<CrmMessage> items;
        long total;
        switch (type) {
            case "sent" -> {
                items = messageMapper.findSent(gymId, userId, offset, size);
                total = messageMapper.countSent(gymId, userId);
            }
            case "notice" -> {
                items = messageMapper.findNotices(gymId, offset, size);
                total = messageMapper.countNotices(gymId);
            }
            default -> throw new IllegalArgumentException("지원하지 않는 type입니다: " + type);
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("items", items);
        data.put("total", total);
        return ApiResponse.ok(data);
    }

    // "읽지않은 쪽지함" — 회원이 지점(GYM) 앞으로 보낸 미확인 쪽지를 회원별로 묶은 대화목록
    @GetMapping("/unread-threads")
    public ApiResponse<List<Map<String, Object>>> unreadThreads(@AuthenticationPrincipal CrmUserDetails principal) {
        log.info("[CrmInbox] GET /api/inbox/unread-threads");
        Long gymId = (principal != null) ? principal.getGymId() : 1L;
        return ApiResponse.ok(messageMapper.findUnreadMemberThreads(gymId));
    }

    @GetMapping("/unread-count")
    public ApiResponse<Long> unreadCount(@AuthenticationPrincipal CrmUserDetails principal) {
        log.info("[CrmInbox] GET /api/inbox/unread-count");
        Long gymId = (principal != null) ? principal.getGymId() : 1L;
        return ApiResponse.ok(messageMapper.countUnreadMemberMessages(gymId));
    }

    @GetMapping("/messages/{id}")
    public ApiResponse<CrmMessage> getOne(@PathVariable String id,
                                           @AuthenticationPrincipal CrmUserDetails principal) {
        log.info("[CrmInbox] GET /api/inbox/messages/{id} - id={}", id);
        Long gymId = (principal != null) ? principal.getGymId() : 1L;
        CrmMessage msg = messageMapper.findById(id, gymId).orElse(null);
        if (msg == null) return ApiResponse.error("메시지를 찾을 수 없습니다.");
        messageMapper.markRead(id, gymId);
        return ApiResponse.ok(msg);
    }

    @PostMapping("/messages")
    public ApiResponse<Void> send(@RequestBody CrmMessage message,
                                   @AuthenticationPrincipal CrmUserDetails principal) {
        log.info("[CrmInbox] POST /api/inbox/messages");
        Long gymId = (principal != null) ? principal.getGymId() : 1L;
        String userId = (principal != null) ? principal.getId() : "unknown";
        String userName = (principal != null) ? principal.getUsername() : "관리자";

        message.setId(UUID.randomUUID().toString());
        message.setGymId(gymId);
        message.setSenderId(userId);
        message.setSenderName(userName);
        message.setSenderType("admin");
        messageMapper.insert(message);
        return ApiResponse.ok();
    }

    // 단체쪽지 — 대상 인원 미리보기
    @GetMapping("/broadcast-count")
    public ApiResponse<Long> broadcastCount(
            @RequestParam(required = false) String gender,
            @RequestParam(required = false) String validity,
            @AuthenticationPrincipal CrmUserDetails principal) {
        Long gymId = (principal != null) ? principal.getGymId() : 1L;
        return ApiResponse.ok(messageMapper.countBroadcastTargets(gymId, gender, validity));
    }

    // 단체쪽지 발송 — 필터(성별/유효만료)에 맞는 회원 전원에게 동일한 내용을 개별 쪽지로 발송
    @PostMapping("/broadcast")
    public ApiResponse<Map<String, Object>> broadcast(
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal CrmUserDetails principal) {
        Long gymId = (principal != null) ? principal.getGymId() : 1L;
        String userId = (principal != null) ? principal.getId() : "unknown";
        String userName = (principal != null) ? principal.getUsername() : "관리자";

        String content = (String) body.get("content");
        if (content == null || content.isBlank()) {
            return ApiResponse.error("내용을 입력해주세요.");
        }
        String gender = (String) body.get("gender");
        String validity = (String) body.get("validity");
        boolean isNotice = Boolean.TRUE.equals(body.get("notice"));

        List<String> targets = messageMapper.findBroadcastTargets(gymId, gender, validity);
        if (targets.isEmpty()) {
            return ApiResponse.error("발송 대상 회원이 없습니다.");
        }
        log.info("[CrmInbox] POST /api/inbox/broadcast - gender={}, validity={}, targetCount={}",
                gender, validity, targets.size());

        for (String memberId : targets) {
            CrmMessage message = new CrmMessage();
            message.setId(UUID.randomUUID().toString());
            message.setGymId(gymId);
            message.setSenderId(userId);
            message.setSenderName(userName);
            message.setSenderType("admin");
            message.setReceiverType("member");
            message.setReceiverId(memberId);
            message.setContent(content);
            message.setNotice(isNotice);
            messageMapper.insert(message);
        }
        return ApiResponse.ok(Map.of("sentCount", targets.size()));
    }

    @PatchMapping("/messages/{id}/read")
    public ApiResponse<Void> markRead(@PathVariable String id,
                                       @AuthenticationPrincipal CrmUserDetails principal) {
        log.info("[CrmInbox] PATCH /api/inbox/messages/{id}/read - id={}", id);
        Long gymId = (principal != null) ? principal.getGymId() : 1L;
        messageMapper.markRead(id, gymId);
        return ApiResponse.ok();
    }

    @PatchMapping("/messages/read-all")
    public ApiResponse<Void> markAllRead(@AuthenticationPrincipal CrmUserDetails principal) {
        log.info("[CrmInbox] PATCH /api/inbox/messages/read-all");
        Long gymId = (principal != null) ? principal.getGymId() : 1L;
        messageMapper.markAllMemberMessagesRead(gymId);
        return ApiResponse.ok();
    }

    @DeleteMapping("/messages/{id}")
    public ApiResponse<Void> delete(@PathVariable String id,
                                     @AuthenticationPrincipal CrmUserDetails principal) {
        log.info("[CrmInbox] DELETE /api/inbox/messages/{id} - id={}", id);
        Long gymId = (principal != null) ? principal.getGymId() : 1L;
        messageMapper.delete(id, gymId);
        return ApiResponse.ok();
    }
}
