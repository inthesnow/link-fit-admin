package com.linkfit.admin.mapper;

import com.linkfit.admin.domain.Member;
import com.linkfit.admin.domain.Staff;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Mapper
public interface StaffMapper {
    List<Staff> findAll(@Param("role") String role, @Param("offset") int offset, @Param("size") int size);
    long count(@Param("role") String role);
    Optional<Staff> findById(@Param("id") String id);
    void update(Staff staff);
    void updateRole(@Param("id") String id, @Param("role") String role);

    // 이름+전화번호로 기존 앱 사용자를 찾아 트레이너로 승격시키는 흐름에서 사용 (역할 무관 조회)
    Optional<Staff> findAppUserByNameAndPhone(@Param("name") String name, @Param("phone") String phone);
    void promoteToTrainer(@Param("id") String id);
    // 트레이너 본인이 앱 가입 시 등록한 지점(user_gym) — CRM 계정 발급 시 관리자 세션 지점 대신 이 값을 우선 사용
    Long findGymIdByUserId(@Param("userId") String userId);
    // 트레이너 권한 회수: 계정 삭제가 아니라 role을 MEMBER로 되돌림
    void revokeTrainer(@Param("id") String id);

    // Sector 13 — 트레이너 CRM 대시보드
    Map<String, Object> findDashboard(@Param("appUserId") String appUserId, @Param("gymId") Long gymId);
    List<Member> findAssignedMembers(@Param("appUserId") String appUserId, @Param("gymId") Long gymId);
}
