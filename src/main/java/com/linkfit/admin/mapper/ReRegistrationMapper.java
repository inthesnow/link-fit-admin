package com.linkfit.admin.mapper;

import com.linkfit.admin.domain.ReRegistration;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Mapper
public interface ReRegistrationMapper {

    List<ReRegistration> findAll(@Param("gymId") Long gymId, @Param("status") String status,
                                  @Param("reason") String reason,
                                  @Param("expiryStatus") String expiryStatus,
                                  @Param("startDate") String startDate, @Param("endDate") String endDate,
                                  @Param("offset") int offset, @Param("size") int size);
    long count(@Param("gymId") Long gymId, @Param("status") String status, @Param("reason") String reason,
               @Param("expiryStatus") String expiryStatus,
               @Param("startDate") String startDate, @Param("endDate") String endDate);

    // 대상자/등록완료/만료 — 실제 회원권 만료일(membership.end_date) 기준 집계
    Map<String, Object> summaryByMembership(@Param("gymId") Long gymId);

    Optional<ReRegistration> findById(@Param("id") String id);

    void insert(ReRegistration r);
    void updateStatus(@Param("id") String id, @Param("status") String status,
                      @Param("resolvedAt") String resolvedAt);
    void updateMemo(@Param("id") String id, @Param("memo") String memo);
    void assign(@Param("id") String id, @Param("assignedTo") String assignedTo);

    // 자동 분류: 이미 존재하는 항목인지 확인 (중복 방지)
    boolean existsByMemberAndReason(@Param("memberId") String memberId,
                                     @Param("gymId") Long gymId,
                                     @Param("reason") String reason);

    // 집계
    int countByStatus(@Param("gymId") Long gymId, @Param("status") String status);
    int countByStatusInPeriod(@Param("gymId") Long gymId, @Param("status") String status,
                               @Param("days") int days);
}
