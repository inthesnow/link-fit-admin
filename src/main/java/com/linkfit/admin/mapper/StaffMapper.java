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
    List<Staff> findAll(@Param("role") String role, @Param("offset") int offset, @Param("size") int size,
                        @Param("gymId") Long gymId);
    List<Staff> findTrainerOptions(@Param("gymId") Long gymId);
    long count(@Param("role") String role, @Param("gymId") Long gymId);
    Optional<Staff> findById(@Param("id") String id, @Param("gymId") Long gymId);
    // 트레이너 등록용 회원 검색 — 이 지점에 승인된(user_gym) 회원 중 이름이 일치하는 후보 목록
    // (이미 TRAINER/ADMIN인 경우는 제외). 관리자가 드롭박스에서 고를 수 있도록 name+phone을 함께 내려준다.
    List<Staff> searchMemberCandidates(@Param("gymId") Long gymId, @Param("keyword") String keyword);
    // 드롭박스에서 선택된 회원이 실제로 이 지점의 승인 회원인지 서버측에서 재확인
    Optional<Staff> findMemberCandidateById(@Param("gymId") Long gymId, @Param("id") String id);
    void promoteToTrainer(@Param("id") String id);
    // 대상 사용자 본인의 소속 지점(user_gym) — 승격시키는 관리자의 gymId와 일치하는지 서비스 계층에서 확인
    Long findGymIdByUserId(@Param("userId") String userId);
    void update(@Param("staff") Staff staff, @Param("gymId") Long gymId);
    void revokeTrainer(@Param("id") String id, @Param("gymId") Long gymId);
    void updateRole(@Param("id") String id, @Param("role") String role, @Param("gymId") Long gymId);

    Map<String, Object> findDashboard(@Param("appUserId") String appUserId, @Param("gymId") Long gymId);
    List<Member> findAssignedMembers(@Param("appUserId") String appUserId, @Param("gymId") Long gymId);
}
