package com.linkfit.admin.mapper;

import com.linkfit.admin.domain.CrmUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

@Mapper
public interface CrmUserMapper {

    Optional<CrmUser> findByBranchCodeAndUsername(
            @Param("branchCode") String branchCode,
            @Param("username")   String username);

    Optional<CrmUser> findById(@Param("id") String id);

    Optional<CrmUser> findByGymIdAndUsername(
            @Param("gymId")    Long gymId,
            @Param("username") String username);

    // 트레이너 지정/해제 시 앱 계정과 연결된 CRM 로그인 계정을 찾기 위함 (중복 발급 방지)
    Optional<CrmUser> findByAppUserId(@Param("appUserId") String appUserId);

    int insert(CrmUser user);

    int updateSecondPassword(@Param("id") String id, @Param("secondPasswordHash") String secondPasswordHash);

    // 최초 로그인 강제 변경(completeFirstLogin)과 별개로, 이미 온보딩을 마친 계정이
    // 설정 화면에서 1차(로그인) 비밀번호만 다시 바꿀 때 사용
    int updatePassword(@Param("id") String id, @Param("passwordHash") String passwordHash);

    // 최초 로그인 강제 변경 완료: 1차 비밀번호 + 2차 비밀번호를 함께 저장하고 플래그 해제
    int completeFirstLogin(@Param("id") String id,
                            @Param("passwordHash") String passwordHash,
                            @Param("secondPasswordHash") String secondPasswordHash);

    int updateLockedCategories(@Param("id") String id, @Param("lockedCategories") String lockedCategories);

    // 트레이너 재지정 시 기존(비활성화된) CRM 계정을 재활성화. gymId도 함께 최신화(트레이너 본인의
    // user_gym 기준 — 최초 발급 이후 소속이 바뀌었을 가능성 대비)
    int reactivateAsTrainer(@Param("id") String id, @Param("gymId") Long gymId);

    // 트레이너 권한 회수 시 CRM 로그인 계정 비활성화 (계정 자체는 보존)
    int deactivateByAppUserId(@Param("appUserId") String appUserId);

    // 지점 관리자가 lof-admin 내에서 직접 만드는 매니저/직원 계정 관리
    List<CrmUser> findByGymIdAndRoles(@Param("gymId") Long gymId, @Param("roles") List<String> roles);
    int updateActive(@Param("id") String id, @Param("gymId") Long gymId, @Param("active") boolean active);
    boolean existsByGymIdAndUsernameAnyStatus(@Param("gymId") Long gymId, @Param("username") String username);
}
