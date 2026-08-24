package com.linkfit.admin.service;

import com.linkfit.admin.domain.CrmUser;

import java.util.List;

/**
 * 지점 관리자가 lof-admin 내에서 직접 만드는 "관리자페이지 전용 계정"(매니저/직원) 관리.
 * 지점코드 발급 시 만들어지는 기본 gym_admin 계정, 트레이너 승격 계정과는 별개의 흐름 —
 * 여기서 만든 계정은 앱 계정(app_user_id)과 연결되지 않은 순수 CRM 로그인 계정이다.
 */
public interface CrmAccountService {
    List<CrmUser> findManagedAccounts(Long gymId);

    /** @throws IllegalArgumentException 이름/아이디/비밀번호 누락, 잘못된 role, 아이디 중복 */
    CrmUser createAccount(String name, String username, String initialPassword, String role, Long gymId);

    void setActive(String id, Long gymId, boolean active);
}
