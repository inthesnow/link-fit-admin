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
            default -> {
                items = messageMapper.findReceived(gymId, userId, offset, size);
                total = messageMapper.countReceived(gymId, userId);
            }
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("items", items);
        data.put("total", total);
        return ApiResponse.ok(data);
    }

    @GetMapping("/unread-count")
    public ApiResponse<Long> unreadCount(@AuthenticationPrincipal CrmUserDetails principal) {
        log.info("[CrmInbox] GET /api/inbox/unread-count");
        Long gymId = (principal != null) ? principal.getGymId() : 1L;
        String userId = (principal != null) ? principal.getId() : "unknown";
        return ApiResponse.ok(messageMapper.countUnread(gymId, userId));
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
        String userId = (principal != null) ? principal.getId() : "unknown";
        messageMapper.markAllRead(gymId, userId);
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
