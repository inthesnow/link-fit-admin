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
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/auth")
public class AuthApiController {

    private static final Logger log = LoggerFactory.getLogger(AuthApiController.class);

    // 2차 비밀번호: 영문 + 숫자 + 특수문자 혼합, 최소 8자리 (대소문자 구분 없음)
    private static final Pattern SECOND_PASSWORD_PATTERN =
            Pattern.compile("^(?=.*[A-Za-z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{8,}$");

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

        // findByBranchCodeAndUsername가 이미 is_active=1로 필터링하므로 여기선 항상 true —
        // 비활성 계정은 이 시점까지 도달하지 않는다 (2026-08-15: 도달 불가능한 분기 정리).

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
                "lockedCategories", List.copyOf(user.lockedCategorySet()),
                "mustChangePassword", user.isMustChangePassword()
        ));
    }

    // ── 최초 로그인 강제 변경: 1차 비밀번호 변경 + 2차 비밀번호 생성을 한 번에 처리 ──
    // 지점코드 발급 시 기본 비밀번호(linkonfit)로 만들어진 계정 전용 — must_change_password가
    // 이미 해제된 계정은 이 엔드포인트로 다시 바꿀 수 없다(일반 비밀번호 변경 기능은 별도 범위).
    @PostMapping("/change-password-first")
    public ApiResponse<?> changePasswordFirst(@AuthenticationPrincipal CrmUserDetails principal,
                                               @RequestBody Map<String, String> body) {
        String currentPassword   = body.get("currentPassword");
        String newPassword       = body.get("newPassword");
        String newSecondPassword = body.get("newSecondPassword");

        if (currentPassword == null || newPassword == null || newSecondPassword == null) {
            return ApiResponse.error("현재 비밀번호, 새 1차 비밀번호, 2차 비밀번호를 모두 입력해주세요.");
        }

        CrmUser user = crmUserService.findById(principal.getId()).orElse(null);
        if (user == null) return ApiResponse.error("사용자를 찾을 수 없습니다.");
        if (!user.isMustChangePassword()) {
            return ApiResponse.error("이미 초기 설정이 완료된 계정입니다.");
        }
        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            return ApiResponse.error("현재 비밀번호가 올바르지 않습니다.");
        }
        if (newPassword.length() < 4) {
            return ApiResponse.error("1차 비밀번호는 최소 4자리 이상이어야 합니다.");
        }
        if (!SECOND_PASSWORD_PATTERN.matcher(newSecondPassword).matches()) {
            return ApiResponse.error("2차 비밀번호는 영문, 숫자, 특수문자를 모두 포함해 최소 8자리 이상이어야 합니다.");
        }

        crmUserService.completeFirstLogin(
                user.getId(),
                passwordEncoder.encode(newPassword),
                passwordEncoder.encode(newSecondPassword));
        return ApiResponse.ok();
    }

    // ── 2차 비밀번호 설정/변경 ──
    // 아직 2차 비밀번호가 없는 계정(최초 설정)은 로그인 세션 자체가 이미 본인 확인이므로
    // 현재(1차) 비밀번호 재확인을 요구하지 않는다. 이미 2차 비밀번호가 설정돼 있는 계정이
    // 그걸 바꾸는 경우에는 기존처럼 현재(1차) 비밀번호로 재확인해야 한다.
    @PostMapping("/second-password")
    public ApiResponse<?> setSecondPassword(@AuthenticationPrincipal CrmUserDetails principal,
                                             @RequestBody Map<String, String> body) {
        String currentPassword    = body.get("currentPassword");
        String newSecondPassword  = body.get("newSecondPassword");
        if (newSecondPassword == null || newSecondPassword.isBlank()) {
            return ApiResponse.error("새 2차 비밀번호를 입력해주세요.");
        }
        if (!SECOND_PASSWORD_PATTERN.matcher(newSecondPassword).matches()) {
            return ApiResponse.error("2차 비밀번호는 영문, 숫자, 특수문자를 모두 포함해 최소 8자리 이상이어야 합니다.");
        }
        CrmUser user = crmUserService.findById(principal.getId()).orElse(null);
        if (user == null) return ApiResponse.error("사용자를 찾을 수 없습니다.");
        boolean isFirstSetup = user.getSecondPasswordHash() == null;
        if (!isFirstSetup) {
            if (currentPassword == null || currentPassword.isBlank()) {
                return ApiResponse.error("현재 비밀번호를 입력해주세요.");
            }
            if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
                return ApiResponse.error("현재 비밀번호가 올바르지 않습니다.");
            }
        }
        crmUserService.updateSecondPassword(user.getId(), passwordEncoder.encode(newSecondPassword));
        return ApiResponse.ok();
    }

    // ── 1차(로그인) 비밀번호 변경 — 온보딩(최초 로그인) 완료 후 설정 화면에서 사용 ──
    @PostMapping("/password")
    public ApiResponse<?> changePassword(@AuthenticationPrincipal CrmUserDetails principal,
                                          @RequestBody Map<String, String> body) {
        String currentPassword = body.get("currentPassword");
        String newPassword     = body.get("newPassword");
        if (currentPassword == null || newPassword == null || newPassword.isBlank()) {
            return ApiResponse.error("현재 비밀번호와 새 비밀번호를 모두 입력해주세요.");
        }
        if (newPassword.length() < 4) {
            return ApiResponse.error("1차 비밀번호는 최소 4자리 이상이어야 합니다.");
        }
        CrmUser user = crmUserService.findById(principal.getId()).orElse(null);
        if (user == null) return ApiResponse.error("사용자를 찾을 수 없습니다.");
        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            return ApiResponse.error("현재 비밀번호가 올바르지 않습니다.");
        }
        crmUserService.updatePassword(user.getId(), passwordEncoder.encode(newPassword));
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
