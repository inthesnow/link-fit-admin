package com.linkfit.admin.service.mybatis;

import com.linkfit.admin.domain.CrmUser;
import com.linkfit.admin.domain.Staff;
import com.linkfit.admin.mapper.CrmUserMapper;
import com.linkfit.admin.mapper.StaffMapper;
import com.linkfit.admin.mapper.UserAuthMapper;
import com.linkfit.admin.service.StaffService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class MyBatisStaffService implements StaffService {

    private final StaffMapper staffMapper;
    private final CrmUserMapper crmUserMapper;
    private final UserAuthMapper userAuthMapper;

    public MyBatisStaffService(StaffMapper staffMapper, CrmUserMapper crmUserMapper, UserAuthMapper userAuthMapper) {
        this.staffMapper = staffMapper;
        this.crmUserMapper = crmUserMapper;
        this.userAuthMapper = userAuthMapper;
    }

    @Override
    public List<Staff> findAll(String role, int page, int size) {
        return staffMapper.findAll(role, page * size, size);
    }

    @Override
    public long count(String role) {
        return staffMapper.count(role);
    }

    @Override
    public Optional<Staff> findById(String id) {
        return staffMapper.findById(id);
    }

    @Override
    public Optional<Staff> findAppUserByNameAndPhone(String name, String phone) {
        return staffMapper.findAppUserByNameAndPhone(name, stripNonDigits(phone));
    }

    @Override
    public Long findTrainerGymId(String id) {
        return staffMapper.findGymIdByUserId(id);
    }

    // 트레이너 지정 시 users.role만 바꾸는 게 아니라, 같은 아이디/비밀번호(앱 계정)로
    // CRM(관리자 페이지)에 로그인할 수 있도록 crm_users 계정도 함께 준비한다.
    // 비밀번호는 crm_users에 별도로 관리하지 않고, 로그인 시점에 user_auth를 직접
    // 검증하므로(AuthApiController 참고) 여기서 넣는 password_hash는 컬럼이 NOT NULL이라
    // 채워두는 초기값일 뿐 실제 인증에는 쓰이지 않는다.
    //
    // gymId는 반드시 트레이너 본인이 앱 가입 시 등록한 지점(user_gym)에서 가져온다 — 승격시키는
    // 관리자의 세션 gymId를 쓰면, 트레이너가 실제로 소속되지 않은 지점코드로 crm_users가 발급되어
    // 나중에 본인 지점코드로 로그인이 안 되는 문제가 생긴다. user_gym이 없으면 컨트롤러 단계에서
    // 이미 걸러지므로(findTrainerGymId) 여기서는 항상 존재한다고 가정한다.
    @Override
    @Transactional
    public Staff promoteToTrainer(String id) {
        staffMapper.promoteToTrainer(id);

        Long trainerGymId = staffMapper.findGymIdByUserId(id);
        if (trainerGymId == null) {
            throw new IllegalStateException("트레이너 본인의 소속 헬스장 정보가 없습니다.");
        }

        Staff staff = staffMapper.findById(id).orElseThrow();
        CrmUser existing = crmUserMapper.findByAppUserId(id).orElse(null);
        if (existing != null) {
            if (!existing.isActive()) {
                crmUserMapper.reactivateAsTrainer(existing.getId(), trainerGymId);
            }
        } else {
            CrmUser crmUser = new CrmUser();
            crmUser.setId(UUID.randomUUID().toString());
            crmUser.setGymId(trainerGymId);
            crmUser.setAppUserId(id);
            crmUser.setName(staff.getName());
            crmUser.setEmail(staff.getEmail());
            crmUser.setUsername(staff.getEmail() != null && !staff.getEmail().isBlank() ? staff.getEmail() : id);
            crmUser.setPasswordHash(userAuthMapper.findEmailPasswordHash(id).orElse(""));
            crmUser.setRole("trainer");
            crmUser.setActive(true);
            crmUserMapper.insert(crmUser);
        }
        return staff;
    }

    @Override
    public Staff update(String id, Staff staff) {
        staff.setId(id);
        staff.setPhone(stripNonDigits(staff.getPhone()));
        staffMapper.update(staff);
        return staff;
    }

    private static String stripNonDigits(String phone) {
        if (phone == null) return null;
        String digits = phone.replaceAll("[^0-9]", "");
        return digits.isEmpty() ? null : digits;
    }

    // "삭제"가 아니라 권한 회수다. 앱 계정(users)은 그대로 두고 role만 MEMBER로 되돌리고,
    // CRM(관리자 페이지) 로그인 계정은 비활성화만 한다 (계정 자체는 보존 — 재지정 시 재활성화됨).
    @Override
    @Transactional
    public void revokeTrainer(String id) {
        staffMapper.revokeTrainer(id);
        crmUserMapper.deactivateByAppUserId(id);
    }

    @Override
    public void updateRole(String id, String role) {
        staffMapper.updateRole(id, role);
    }
}
