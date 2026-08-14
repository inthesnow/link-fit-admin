package com.linkfit.admin.controller.api;

import com.linkfit.admin.common.ApiResponse;
import com.linkfit.admin.domain.CrmAnnouncement;
import com.linkfit.admin.mapper.CrmAnnouncementMapper;
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
@RequestMapping("/api/announcements")
public class AnnouncementApiController {

    private static final Logger log = LoggerFactory.getLogger(AnnouncementApiController.class);

    private final CrmAnnouncementMapper mapper;

    public AnnouncementApiController(CrmAnnouncementMapper mapper) {
        this.mapper = mapper;
    }

    @GetMapping
    public ApiResponse<Map<String, Object>> list(
            @RequestParam(defaultValue = "") String target,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal CrmUserDetails principal) {
        log.info("[Announcement] GET /api/announcements - target={}, page={}", target, page);
        Long gymId = gymId(principal);
        int offset = page * size;
        List<CrmAnnouncement> items = mapper.findAll(gymId, target, offset, size);
        long total = mapper.count(gymId, target);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("items", items);
        data.put("total", total);
        return ApiResponse.ok(data);
    }

    @GetMapping("/{id}")
    public ApiResponse<CrmAnnouncement> getOne(@PathVariable String id,
                                                @AuthenticationPrincipal CrmUserDetails principal) {
        log.info("[Announcement] GET /api/announcements/{id} - id={}", id);
        return mapper.findById(id, gymId(principal))
                .map(ApiResponse::ok)
                .orElse(ApiResponse.error("공지를 찾을 수 없습니다."));
    }

    @PostMapping
    public ApiResponse<Void> create(@RequestBody CrmAnnouncement announcement,
                                     @AuthenticationPrincipal CrmUserDetails principal) {
        log.info("[Announcement] POST /api/announcements");
        announcement.setId(UUID.randomUUID().toString());
        announcement.setGymId(gymId(principal));
        announcement.setAuthorId(principal != null ? principal.getId() : null);
        mapper.insert(announcement);
        return ApiResponse.ok();
    }

    @PatchMapping("/{id}/send")
    public ApiResponse<Void> markSent(@PathVariable String id,
                                       @AuthenticationPrincipal CrmUserDetails principal) {
        log.info("[Announcement] PATCH /api/announcements/{id}/send - id={}", id);
        if (mapper.markSent(id, gymId(principal)) == 0) {
            return ApiResponse.error("공지를 찾을 수 없습니다.");
        }
        return ApiResponse.ok();
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable String id,
                                     @AuthenticationPrincipal CrmUserDetails principal) {
        log.info("[Announcement] DELETE /api/announcements/{id} - id={}", id);
        if (mapper.delete(id, gymId(principal)) == 0) {
            return ApiResponse.error("공지를 찾을 수 없습니다.");
        }
        return ApiResponse.ok();
    }

    private Long gymId(CrmUserDetails principal) {
        return principal != null ? principal.getGymId() : 1L;
    }
}
