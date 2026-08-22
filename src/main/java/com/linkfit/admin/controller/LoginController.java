package com.linkfit.admin.controller;

import com.linkfit.admin.domain.CrmUser;
import com.linkfit.admin.security.CrmUserDetails;
import com.linkfit.admin.service.CrmUserService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class LoginController {

    private final CrmUserService crmUserService;

    public LoginController(CrmUserService crmUserService) {
        this.crmUserService = crmUserService;
    }

    @GetMapping({"/", "/login"})
    public String loginPage() {
        return "login";
    }

    @GetMapping("/dashboard")
    public String dashboard() {
        return "dashboard";
    }

    @GetMapping("/change-password-first")
    public String changePasswordFirst(@AuthenticationPrincipal CrmUserDetails principal) {
        if (principal != null) {
            CrmUser user = crmUserService.findById(principal.getId()).orElse(null);
            if (user == null || !user.isMustChangePassword()) {
                return "redirect:/dashboard";
            }
        }
        return "first-login";
    }
}
