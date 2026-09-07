package com.linkfit.admin.mapper;

import com.linkfit.admin.domain.GymJoinRequest;
import com.linkfit.admin.domain.GymJoinRequestLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

@Mapper
public interface GymJoinRequestMapper {
    List<GymJoinRequest> findAll(@Param("gymId") Long gymId, @Param("status") String status,
                                  @Param("offset") int offset, @Param("size") int size);
    long count(@Param("gymId") Long gymId, @Param("status") String status);
    // 요약 카드 "승인됨" 전용 — 승인된 user_gym 중 실제로 앱 회원가입(user_auth)까지 마친 사람만.
    // CRM 일괄등록 등으로 만들어진, 앱에 로그인해본 적 없는 회원까지 승인됨에 잡히는 걸 막기 위함.
    long countApprovedAppUsers(@Param("gymId") Long gymId);
    Optional<GymJoinRequest> findById(@Param("id") Long id);

    int approve(@Param("id") Long id);
    int reject(@Param("id") Long id);

    void insertLog(@Param("userId") String userId, @Param("gymId") Long gymId,
                    @Param("action") String action, @Param("memo") String memo,
                    @Param("actorId") String actorId);
    List<GymJoinRequestLog> findLogs(@Param("userId") String userId, @Param("gymId") Long gymId);
}
