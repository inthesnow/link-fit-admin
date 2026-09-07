package com.linkfit.admin.security;

import com.linkfit.admin.domain.CrmUser;
import com.linkfit.admin.service.CrmUserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.List;

/**
 * 지점코드 발급으로 만들어진 gym_admin 계정(기본 비밀번호 공유)이 최초 로그인 시
 * 1차 비밀번호 변경 + 2차 비밀번호 생성을 완료하기 전까지는 다른 모든 페이지/API
 * 접근을 서버 단에서 차단한다. LockedCategoryInterceptor(카테고리별 2차 비밀번호 재확인)와
 * 별개의, 전역 온보딩 게이트다.
 */
@Component
public class MustChangePasswordInterceptor implements HandlerInterceptor {

    private static final List<String> ALLOWED_PREFIXES = List.of(
            "/change-password-first", "/api/auth/change-password-first",
            "/api/auth/login", "/api/auth/logout",
            "/css/", "/js/", "/image/", "/favicon.svg", "/error"
    );

    private final CrmUserService crmUserService;

    public MustChangePasswordInterceptor(CrmUserService crmUserService) {
        this.crmUserService = crmUserService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String path = request.getRequestURI();
        if (isAllowed(path)) return true;

        if (!(SecurityContextHolder.getContext().getAuthentication() != null
                && SecurityContextHolder.getContext().getAuthentication().getPrincipal() instanceof CrmUserDetails principal)) {
            return true; // 미인증 요청은 SecurityConfig가 처리
        }

        CrmUser user = crmUserService.findById(principal.getId()).orElse(null);
        if (user == null || !user.isMustChangePassword()) {
            return true;
        }

        if (path.startsWith("/api/")) {
            response.setStatus(423); // Locked
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(
                    "{\"success\":false,\"mustChangePassword\":true,"
                            + "\"message\":\"최초 로그인 비밀번호 변경을 먼저 완료해주세요.\"}");
        } else {
            response.sendRedirect("/change-password-first");
        }
        return false;
    }

    private boolean isAllowed(String path) {
        for (String prefix : ALLOWED_PREFIXES) {
            if (path.equals(prefix) || path.startsWith(prefix)) return true;
        }
        return false;
    }
}
