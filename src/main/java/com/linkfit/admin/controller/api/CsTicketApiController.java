package com.linkfit.admin.controller.api;

import com.linkfit.admin.common.ApiResponse;
import com.linkfit.admin.domain.CrmCsTicket;
import com.linkfit.admin.mapper.CrmCsTicketMapper;
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
@RequestMapping("/api/cs")
public class CsTicketApiController {

    private static final Logger log = LoggerFactory.getLogger(CsTicketApiController.class);

    private final CrmCsTicketMapper mapper;

    public CsTicketApiController(CrmCsTicketMapper mapper) {
        this.mapper = mapper;
    }

    @GetMapping("/tickets")
    public ApiResponse<Map<String, Object>> list(
            @RequestParam(defaultValue = "") String status,
            @RequestParam(defaultValue = "") String category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal CrmUserDetails principal) {
        log.info("[CsTicket] GET /api/cs/tickets - status={}, category={}", status, category);
        Long gymId = gymId(principal);
        int offset = page * size;
        List<CrmCsTicket> items = mapper.findAll(gymId, status, category, offset, size);
        long total = mapper.count(gymId, status, category);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("items", items);
        data.put("total", total);
        return ApiResponse.ok(data);
    }

    @GetMapping("/tickets/summary")
    public ApiResponse<Map<String, Object>> summary(@AuthenticationPrincipal CrmUserDetails principal) {
        log.info("[CsTicket] GET /api/cs/tickets/summary");
        Long gymId = gymId(principal);
        String[] statuses = {"received", "checking", "processing", "answered", "closed"};
        Map<String, Object> data = new LinkedHashMap<>();
        for (String s : statuses) data.put(s, mapper.countByStatus(gymId, s));
        return ApiResponse.ok(data);
    }

    @GetMapping("/tickets/{id}")
    public ApiResponse<CrmCsTicket> getOne(@PathVariable String id,
                                            @AuthenticationPrincipal CrmUserDetails principal) {
        log.info("[CsTicket] GET /api/cs/tickets/{id} - id={}", id);
        return mapper.findById(id, gymId(principal))
                .map(ApiResponse::ok)
                .orElse(ApiResponse.error("티켓을 찾을 수 없습니다."));
    }

    @PostMapping("/tickets")
    public ApiResponse<Void> create(@RequestBody CrmCsTicket ticket,
                                     @AuthenticationPrincipal CrmUserDetails principal) {
        log.info("[CsTicket] POST /api/cs/tickets");
        ticket.setId(UUID.randomUUID().toString());
        ticket.setGymId(gymId(principal));
        mapper.insert(ticket);
        return ApiResponse.ok();
    }

    @PatchMapping("/tickets/{id}/status")
    public ApiResponse<Void> updateStatus(@PathVariable String id,
                                           @RequestBody Map<String, String> body,
                                           @AuthenticationPrincipal CrmUserDetails principal) {
        log.info("[CsTicket] PATCH /api/cs/tickets/{id}/status - id={}", id);
        if (mapper.updateStatus(id, body.get("status"), gymId(principal)) == 0) {
            return ApiResponse.error("티켓을 찾을 수 없습니다.");
        }
        return ApiResponse.ok();
    }

    @PatchMapping("/tickets/{id}/assign")
    public ApiResponse<Void> assign(@PathVariable String id,
                                     @RequestBody Map<String, String> body,
                                     @AuthenticationPrincipal CrmUserDetails principal) {
        log.info("[CsTicket] PATCH /api/cs/tickets/{id}/assign - id={}", id);
        if (mapper.assign(id, body.get("assignedTo"), gymId(principal)) == 0) {
            return ApiResponse.error("티켓을 찾을 수 없습니다.");
        }
        return ApiResponse.ok();
    }

    @PatchMapping("/tickets/{id}/respond")
    public ApiResponse<Void> respond(@PathVariable String id,
                                      @RequestBody Map<String, String> body,
                                      @AuthenticationPrincipal CrmUserDetails principal) {
        log.info("[CsTicket] PATCH /api/cs/tickets/{id}/respond - id={}", id);
        if (mapper.respond(id, body.get("response"), gymId(principal)) == 0) {
            return ApiResponse.error("티켓을 찾을 수 없습니다.");
        }
        return ApiResponse.ok();
    }

    private Long gymId(CrmUserDetails principal) {
        return principal != null ? principal.getGymId() : 1L;
    }
}
