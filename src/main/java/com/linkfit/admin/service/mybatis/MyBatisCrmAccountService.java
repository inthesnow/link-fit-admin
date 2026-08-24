package com.linkfit.admin.service.mybatis;

import com.linkfit.admin.domain.CrmUser;
import com.linkfit.admin.mapper.CrmUserMapper;
import com.linkfit.admin.service.CrmAccountService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class MyBatisCrmAccountService implements CrmAccountService {

    private static final Set<String> CREATABLE_ROLES = Set.of("manager", "employee");

    private final CrmUserMapper crmUserMapper;
    private final PasswordEncoder passwordEncoder;

    public MyBatisCrmAccountService(CrmUserMapper crmUserMapper, PasswordEncoder passwordEncoder) {
        this.crmUserMapper = crmUserMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public List<CrmUser> findManagedAccounts(Long gymId) {
        return crmUserMapper.findByGymIdAndRoles(gymId, List.copyOf(CREATABLE_ROLES));
    }

    @Override
    public CrmUser createAccount(String name, String username, String initialPassword, String role, Long gymId) {
        if (name == null || name.isBlank() || username == null || username.isBlank()
                || initialPassword == null || initialPassword.isBlank()) {
            throw new IllegalArgumentException("이름, 아이디, 초기 비밀번호를 모두 입력해주세요.");
        }
        if (!CREATABLE_ROLES.contains(role)) {
            throw new IllegalArgumentException("권한은 매니저 또는 직원만 선택할 수 있습니다.");
        }
        if (initialPassword.length() < 4) {
            throw new IllegalArgumentException("초기 비밀번호는 최소 4자리 이상이어야 합니다.");
        }
        if (crmUserMapper.existsByGymIdAndUsernameAnyStatus(gymId, username)) {
            throw new IllegalArgumentException("이미 사용 중인 아이디입니다.");
        }

        CrmUser account = new CrmUser();
        account.setId(UUID.randomUUID().toString());
        account.setGymId(gymId);
        account.setName(name);
        account.setUsername(username);
        account.setPasswordHash(passwordEncoder.encode(initialPassword));
        account.setRole(role);
        account.setActive(true);
        // 공유 초기 비밀번호로 만들어지므로 gym_admin 신규 발급과 동일하게 최초 로그인 시
        // 1/2차 비밀번호를 다시 설정하도록 강제한다(MustChangePasswordInterceptor).
        account.setMustChangePassword(true);
        crmUserMapper.insert(account);
        return account;
    }

    @Override
    public void setActive(String id, Long gymId, boolean active) {
        int updated = crmUserMapper.updateActive(id, gymId, active);
        if (updated == 0) {
            throw new IllegalArgumentException("계정을 찾을 수 없습니다.");
        }
    }
}
