package com.linkfit.admin.security;

import com.linkfit.admin.domain.CrmUser;
import com.linkfit.admin.service.CrmUserService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Map;

/**
 * 2차 비밀번호로 잠긴 카테고리에 대한 접근을 서버 단에서도 차단한다.
 * 실제 진입 차단(비밀번호 모달)은 사이드바 JS가 먼저 처리하지만, 이 인터셉터가
 * 카테고리 페이지/API 경로에 대한 최종 방어선 역할을 한다.
 */
@Component
public class LockedCategoryInterceptor implements HandlerInterceptor {

    private final CrmUserService crmUserService;
    private final JwtUtil jwtUtil;

    public LockedCategoryInterceptor(CrmUserService crmUserService, JwtUtil jwtUtil) {
        this.crmUserService = crmUserService;
        this.jwtUtil = jwtUtil;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!(SecurityContextHolder.getContext().getAuthentication() != null
                && SecurityContextHolder.getContext().getAuthentication().getPrincipal() instanceof CrmUserDetails principal)) {
            return true; // 미인증 요청은 SecurityConfig가 처리
        }

        String path = request.getRequestURI();
        String category = matchCategory(path);
        if (category == null) return true; // 잠금 대상 경로가 아님

        CrmUser user = crmUserService.findById(principal.getId()).orElse(null);
        if (user == null) return true;

        // 직원(employee) 계정은 2차 비밀번호 잠금 대상 카테고리를 영구적으로 볼 수 없다 —
        // 본인이 2차 비밀번호를 알고 있어도(설정했더라도) 해제 불가. lockedCategorySet()이나
        // 언락 쿠키를 아예 확인하지 않고 항상 차단한다. 단, 대시보드는 예외(2026-08-24 요청) —
        // 랜딩 화면이라 항상 볼 수 있어야 하고, 대신 그 안에서 다른 잠금 카테고리로 이어지는
        // 링크/API를 타면 그건 해당 카테고리 규칙대로 여전히 막힌다.
        boolean employeeBlocked = "employee".equals(user.getRole()) && !"dashboard".equals(category);
        if (!employeeBlocked) {
            if (!user.lockedCategorySet().contains(category)) {
                return true; // 이 사용자에게는 잠기지 않은 카테고리
            }
            String unlockToken = extractCookie(request, JwtUtil.UNLOCK_COOKIE_NAME);
            if (unlockToken != null && jwtUtil.isValid(unlockToken)
                    && user.getId().equals(jwtUtil.getSubject(unlockToken))
                    && category.equals(jwtUtil.getUnlockCategory(unlockToken))) {
                return true; // 해당 카테고리 잠금 해제 상태
            }
        }

        if (path.startsWith("/api/")) {
            response.setStatus(423); // Locked
            response.setContentType("application/json;charset=UTF-8");
            String message = employeeBlocked ? "권한이 없습니다." : "2차 비밀번호 확인이 필요합니다.";
            response.getWriter().write(
                    "{\"success\":false,\"locked\":true,\"category\":\"" + category
                            + "\",\"message\":\"" + message + "\"}");
        } else if (employeeBlocked) {
            writeEmployeeBlockedPage(response);
        } else {
            // 사이드바 클릭이 아니라 URL 직접 접근/새로고침/북마크로 들어온 경우 —
            // 사이드바 JS의 클릭 가로채기가 동작하지 않으므로, 이 페이지 자체에
            // 2차 비밀번호 입력창과 로그아웃 버튼을 그대로 제공해야 막다른 길이 되지 않는다.
            response.setContentType("text/html;charset=UTF-8");
            String safePath = path.replace("\\", "").replace("\"", "&quot;").replace("'", "");
            String safeCategory = category.replace("'", "");
            response.getWriter().write(
                    "<!DOCTYPE html><html><head><meta charset=\"UTF-8\"><title>2차 비밀번호 확인</title></head>"
                            + "<body style=\"font-family:sans-serif;display:flex;align-items:center;justify-content:center;"
                            + "height:100vh;margin:0;background:#F6F8FA\">"
                            + "<div style=\"background:#fff;border:1px solid #D0D7DE;border-radius:10px;padding:32px;width:320px\">"
                            + "<h2 style=\"margin:0 0 8px;font-size:16px;color:#1F2328\">2차 비밀번호 확인</h2>"
                            + "<p style=\"margin:0 0 16px;font-size:13px;color:#636C76\">이 메뉴는 2차 비밀번호로 잠겨 있습니다.</p>"
                            + "<input type=\"password\" id=\"pw\" placeholder=\"2차 비밀번호\" autofocus "
                            + "style=\"width:100%;box-sizing:border-box;padding:8px 10px;border:1px solid #D0D7DE;"
                            + "border-radius:8px;margin-bottom:8px;font-size:14px\" onkeyup=\"if(event.key==='Enter')submitPw()\">"
                            + "<div id=\"err\" style=\"color:#CF222E;font-size:12px;margin-bottom:8px;display:none\"></div>"
                            + "<button onclick=\"submitPw()\" style=\"width:100%;padding:9px;border:none;border-radius:8px;"
                            + "background:#0969DA;color:#fff;cursor:pointer;font-size:13px;margin-bottom:8px\">확인</button>"
                            + "<div style=\"display:flex;justify-content:space-between;font-size:12px\">"
                            + "<a href=\"/dashboard\" style=\"color:#636C76\">대시보드로 이동</a>"
                            + "<a href=\"#\" onclick=\"doLogout();return false;\" style=\"color:#636C76\">로그아웃</a>"
                            + "</div></div>"
                            + "<script>"
                            + "async function submitPw(){"
                            + "  const pw=document.getElementById('pw').value;"
                            + "  const err=document.getElementById('err');"
                            + "  if(!pw){err.textContent='2차 비밀번호를 입력해주세요.';err.style.display='';return;}"
                            + "  try{"
                            + "    const res=await fetch('/api/auth/second-password/verify',{method:'POST',"
                            + "      headers:{'Content-Type':'application/json'},"
                            + "      body:JSON.stringify({category:'" + safeCategory + "',password:pw})});"
                            + "    const json=await res.json();"
                            + "    if(!json.success){err.textContent=json.message||'확인에 실패했습니다.';err.style.display='';return;}"
                            + "    window.location.href='" + safePath + "';"
                            + "  }catch(e){err.textContent='네트워크 오류가 발생했습니다.';err.style.display='';}"
                            + "}"
                            + "function doLogout(){"
                            + "  fetch('/api/auth/logout',{method:'POST'}).then(()=>{window.location.href='/login?logout';});"
                            + "}"
                            + "</script>"
                            + "</body></html>");
        }
        return false;
    }

    // 직원 계정은 비밀번호를 입력해도 해제될 수 없으므로, 2차 비밀번호 입력창이 있는
    // 기존 잠금 페이지가 아니라 접근 불가 안내만 보여준다. 대시보드는 이제 예외라서
    // 항상 접근 가능 — "돌아가기" 링크로 안전하게 쓸 수 있다.
    private void writeEmployeeBlockedPage(HttpServletResponse response) throws java.io.IOException {
        response.setContentType("text/html;charset=UTF-8");
        response.getWriter().write(
                "<!DOCTYPE html><html><head><meta charset=\"UTF-8\"><title>권한이 없습니다</title></head>"
                        + "<body style=\"font-family:sans-serif;display:flex;align-items:center;justify-content:center;"
                        + "height:100vh;margin:0;background:#F6F8FA\">"
                        + "<div style=\"background:#fff;border:1px solid #D0D7DE;border-radius:10px;padding:32px;width:320px;text-align:center\">"
                        + "<h2 style=\"margin:0 0 8px;font-size:16px;color:#1F2328\">권한이 없습니다</h2>"
                        + "<p style=\"margin:0 0 20px;font-size:13px;color:#636C76\">직원 계정으로는 이 메뉴를 볼 수 없습니다.</p>"
                        + "<div style=\"display:flex;justify-content:center;gap:16px;font-size:12px\">"
                        + "<a href=\"/dashboard\" style=\"color:#0969DA\">대시보드로 이동</a>"
                        + "<a href=\"#\" onclick=\"fetch('/api/auth/logout',{method:'POST'}).then(()=>{window.location.href='/login?logout';});return false;\" "
                        + "style=\"color:#636C76\">로그아웃</a>"
                        + "</div></div></body></html>");
    }

    private String matchCategory(String path) {
        for (Map.Entry<String, java.util.List<String>> e : LockableCategories.CATEGORY_PATHS.entrySet()) {
            for (String prefix : e.getValue()) {
                if (path.equals(prefix) || path.startsWith(prefix + "/")) {
                    return e.getKey();
                }
            }
        }
        return null;
    }

    private String extractCookie(HttpServletRequest request, String name) {
        if (request.getCookies() == null) return null;
        for (Cookie c : request.getCookies()) {
            if (name.equals(c.getName())) return c.getValue();
        }
        return null;
    }
}
