package com.linkfit.admin.security;

import java.util.List;
import java.util.Map;

/**
 * 2차 비밀번호로 잠글 수 있는 사이드바 카테고리 목록과, 각 카테고리에 대응하는
 * 페이지/API 경로 접두어. 사이드바(fragments/sidebar.html)의 activePage 값과 1:1로 맞춘다.
 */
public final class LockableCategories {

    public static final Map<String, List<String>> CATEGORY_PATHS = Map.ofEntries(
            Map.entry("dashboard",       List.of("/dashboard", "/api/dashboard")),
            Map.entry("members",         List.of("/members", "/api/members", "/api/memberships")),
            Map.entry("pt",              List.of("/pt", "/api/pt")),
            Map.entry("reregistration",  List.of("/reregistration", "/api/reregistration")),
            Map.entry("attendance",      List.of("/attendance", "/api/attendance")),
            Map.entry("products",        List.of("/products", "/api/products")),
            Map.entry("feedback",        List.of("/feedback", "/api/feedback")),
            Map.entry("consults",        List.of("/consults", "/api/consults")),
            Map.entry("inbox",           List.of("/inbox", "/api/inbox")),
            Map.entry("cs",              List.of("/cs", "/api/cs")),
            Map.entry("announcements",   List.of("/announcements", "/api/announcements")),
            Map.entry("staff",           List.of("/staff", "/api/staff")),
            Map.entry("crm-sales",       List.of("/crm-sales", "/api/crm-sales")),
            Map.entry("revenue",         List.of("/revenue", "/api/revenue")),
            Map.entry("settings",        List.of("/settings", "/api/settings"))
    );

    private LockableCategories() {}
}
