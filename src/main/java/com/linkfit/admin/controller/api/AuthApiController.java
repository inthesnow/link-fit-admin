package com.linkfit.admin.controller.api;

import com.linkfit.admin.common.ApiResponse;
import com.linkfit.admin.domain.CrmUser;
import com.linkfit.admin.mapper.UserAuthMapper;
import com.linkfit.admin.security.CrmUserDetails;
import com.linkfit.admin.security.JwtUtil;
import com.linkfit.admin.security.LockableCategories;
import com.linkfit.admin.service.CrmUserService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/auth")
public class AuthApiController {

    private static final Logger log = LoggerFactory.getLogger(AuthApiController.class);

    private final CrmUserService crmUserService;
    private final UserAuthMapper userAuthMapper;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;
    private final String cookieName;

    public AuthApiController(CrmUserService crmUserService,
                             UserAuthMapper userAuthMapper,
                             JwtUtil jwtUtil,
                             PasswordEncoder passwordEncoder,
                             @Value("${app.jwt.cookie-name}") String cookieName) {
        this.crmUserService  = crmUserService;
        this.userAuthMapper  = userAuthMapper;
        this.jwtUtil         = jwtUtil;
        this.passwordEncoder = passwordEncoder;
        this.cookieName      = cookieName;
    }

    @PostMapping("/login")
    public ApiResponse<?> login(@RequestBody Map<String, String> body,
                                HttpServletResponse response) {
        log.info("[Auth] POST /api/auth/login");
        String branchCode = body.get("branchCode");
        String username   = body.get("username");
        String password   = body.get("password");

        if (branchCode == null || username == null || password == null) {
            return ApiResponse.error("지점코드, 아이디, 비밀번호를 모두 입력해주세요.");
        }

        Optional<CrmUser> userOpt =
                crmUserService.findByBranchCodeAndUsername(branchCode.toUpperCase(), username);

        if (userOpt.isEmpty()) {
            return ApiResponse.error("지점코드 또는 아이디가 올바르지 않습니다.");
        }

        CrmUser user = userOpt.get();

        // 트레이너 지정으로 발급된 계정은 별도 비밀번호를 두지 않고, 앱 로그인 비밀번호를
        // 그대로 검증한다 (트레이너가 앱 비밀번호를 바꿔도 항상 최신 값으로 확인됨).
        String passwordHashToCheck = user.getPasswordHash();
        if (user.getAppUserId() != null) {
            Optional<String> appHash = userAuthMapper.findEmailPasswordHash(user.getAppUserId());
            if (appHash.isEmpty()) {
                return ApiResponse.error("앱 계정에 이메일/비밀번호 로그인이 설정되어 있지 않습니다.");
            }
            passwordHashToCheck = appHash.get();
        }

        if (!passwordEncoder.matches(password, passwordHashToCheck)) {
            return ApiResponse.error("비밀번호가 올바르지 않습니다.");
        }

        if (!user.isActive()) {
            return ApiResponse.error("비활성화된 계정입니다.");
        }

        String token = jwtUtil.generateToken(
                user.getId(), user.getBranchCode(), user.getUsername(),
                user.getRole(), user.getGymId());

        Cookie cookie = new Cookie(cookieName, token);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(86400);    // 24h
        response.addCookie(cookie);

        return ApiResponse.ok(Map.of(
                "id",         user.getId(),
                "name",       user.getName(),
                "role",       user.getRole(),
                "branchCode", user.getBranchCode(),
                "gymId",      user.getGymId()
        ));
    }

    @PostMapping("/logout")
    public ApiResponse<?> logout(HttpServletResponse response) {
        log.info("[Auth] POST /api/auth/logout");
        Cookie cookie = new Cookie(cookieName, "");
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
        return ApiResponse.ok();
    }

    @GetMapping("/me")
    public ApiResponse<?> me(@AuthenticationPrincipal CrmUserDetails principal) {
        CrmUser user = crmUserService.findById(principal.getId()).orElse(null);
        if (user == null) return ApiResponse.error("사용자를 찾을 수 없습니다.");
        return ApiResponse.ok(Map.of(
                "id",              user.getId(),
                "name",            user.getName(),
                "role",            user.getRole(),
                "branchCode",      user.getBranchCode(),
                "gymId",           user.getGymId(),
                "hasSecondPassword", user.getSecondPasswordHash() != null,
                "lockedCategories", List.copyOf(user.lockedCategorySet())
        ));
    }

    // ── 2차 비밀번호 설정/변경 (본인 로그인 비밀번호 재확인 필요) ──
    @PostMapping("/second-password")
    public ApiResponse<?> setSecondPassword(@AuthenticationPrincipal CrmUserDetails principal,
                                             @RequestBody Map<String, String> body) {
        String currentPassword    = body.get("currentPassword");
        String newSecondPassword  = body.get("newSecondPassword");
        if (currentPassword == null || newSecondPassword == null || newSecondPassword.isBlank()) {
            return ApiResponse.error("현재 비밀번호와 새 2차 비밀번호를 모두 입력해주세요.");
        }
        CrmUser user = crmUserService.findById(principal.getId()).orElse(null);
        if (user == null) return ApiResponse.error("사용자를 찾을 수 없습니다.");
        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            return ApiResponse.error("현재 비밀번호가 올바르지 않습니다.");
        }
        crmUserService.updateSecondPassword(user.getId(), passwordEncoder.encode(newSecondPassword));
        return ApiResponse.ok();
    }

    // ── 카테고리 잠금 해제: 2차 비밀번호 검증 성공 시 해당 카테고리 한정 단기 쿠키 발급 ──
    @PostMapping("/second-password/verify")
    public ApiResponse<?> verifySecondPassword(@AuthenticationPrincipal CrmUserDetails principal,
                                                @RequestBody Map<String, String> body,
                                                HttpServletResponse response) {
        String category = body.get("category");
        String password  = body.get("password");
        if (category == null || !LockableCategories.CATEGORY_PATHS.containsKey(category)) {
            return ApiResponse.error("알 수 없는 카테고리입니다.");
        }
        CrmUser user = crmUserService.findById(principal.getId()).orElse(null);
        if (user == null) return ApiResponse.error("사용자를 찾을 수 없습니다.");
        if (user.getSecondPasswordHash() == null) {
            return ApiResponse.error("먼저 2차 비밀번호를 설정해주세요.");
        }
        if (password == null || !passwordEncoder.matches(password, user.getSecondPasswordHash())) {
            return ApiResponse.error("2차 비밀번호가 올바르지 않습니다.");
        }

        String unlockToken = jwtUtil.generateUnlockToken(user.getId(), category);
        Cookie cookie = new Cookie(JwtUtil.UNLOCK_COOKIE_NAME, unlockToken);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(10 * 60); // 10분
        response.addCookie(cookie);
        return ApiResponse.ok();
    }

    // ── 잠글 카테고리 목록 저장 (본인 계정 설정) ──
    @PutMapping("/security-settings")
    public ApiResponse<?> saveSecuritySettings(@AuthenticationPrincipal CrmUserDetails principal,
                                                @RequestBody Map<String, List<String>> body) {
        List<String> categories = body.getOrDefault("lockedCategories", List.of());
        for (String c : categories) {
            if (!LockableCategories.CATEGORY_PATHS.containsKey(c)) {
                return ApiResponse.error("알 수 없는 카테고리: " + c);
            }
        }
        crmUserService.updateLockedCategories(principal.getId(), String.join(",", categories));
        return ApiResponse.ok();
    }
}
