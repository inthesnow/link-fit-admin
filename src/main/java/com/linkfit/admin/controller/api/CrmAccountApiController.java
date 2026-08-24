package com.linkfit.admin.controller.api;

import com.linkfit.admin.common.ApiResponse;
import com.linkfit.admin.domain.CrmUser;
import com.linkfit.admin.security.CrmUserDetails;
import com.linkfit.admin.service.CrmAccountService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 지점 관리자가 lof-admin 내에서 직접 만드는 매니저/직원 계정 관리.
 * 지점코드 발급(potal)이나 트레이너 승격과는 별개의, 순수 CRM 로그인 전용 계정.
 */
@RestController
@RequestMapping("/api/crm-accounts")
public class CrmAccountApiController {

    private static final Logger log = LoggerFactory.getLogger(CrmAccountApiController.class);

    private final CrmAccountService crmAccountService;

    public CrmAccountApiController(CrmAccountService crmAccountService) {
        this.crmAccountService = crmAccountService;
    }

    @GetMapping
    public ApiResponse<List<Map<String, Object>>> list(@AuthenticationPrincipal CrmUserDetails principal) {
        List<CrmUser> accounts = crmAccountService.findManagedAccounts(principal.getGymId());
        List<Map<String, Object>> data = accounts.stream().map(a -> {
            Map<String, Object> m = new java.util.LinkedHashMap<>();
            m.put("id", a.getId());
            m.put("name", a.getName());
            m.put("username", a.getUsername());
            m.put("role", a.getRole());
            m.put("active", a.isActive());
            m.put("mustChangePassword", a.isMustChangePassword());
            m.put("createdAt", a.getCreatedAt());
            return m;
        }).toList();
        return ApiResponse.ok(data);
    }

    @PostMapping
    public ApiResponse<?> create(@RequestBody Map<String, String> body,
                                  @AuthenticationPrincipal CrmUserDetails principal) {
        log.info("[CrmAccount] POST /api/crm-accounts - username={}, role={}", body.get("username"), body.get("role"));
        try {
            crmAccountService.createAccount(
                    body.get("name"), body.get("username"), body.get("initialPassword"),
                    body.get("role"), principal.getGymId());
            return ApiResponse.ok();
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    @PatchMapping("/{id}/active")
    public ApiResponse<?> setActive(@PathVariable String id, @RequestBody Map<String, Boolean> body,
                                     @AuthenticationPrincipal CrmUserDetails principal) {
        Boolean active = body.get("active");
        if (active == null) return ApiResponse.error("active 값이 필요합니다.");
        try {
            crmAccountService.setActive(id, principal.getGymId(), active);
            return ApiResponse.ok();
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(e.getMessage());
        }
    }
}
