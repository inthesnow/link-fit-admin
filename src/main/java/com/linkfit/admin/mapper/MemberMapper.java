package com.linkfit.admin.mapper;

import com.linkfit.admin.domain.Member;
import com.linkfit.admin.domain.MemberFreeze;
import com.linkfit.admin.domain.MemberTicket;
import com.linkfit.admin.domain.Membership;
import com.linkfit.admin.domain.PtMember;
import com.linkfit.admin.domain.TicketLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Mapper
public interface MemberMapper {
    List<Member> findAll(@Param("keyword") String keyword, @Param("status") String status,
                         @Param("tier") String tier, @Param("gymId") Long gymId,
                         @Param("trainerIds") List<String> trainerIds,
                         @Param("minDaysLeft") Integer minDaysLeft, @Param("maxDaysLeft") Integer maxDaysLeft,
                         @Param("minPtRemaining") Integer minPtRemaining, @Param("minAbsentDays") Integer minAbsentDays,
                         @Param("offset") int offset, @Param("size") int size);
    long count(@Param("keyword") String keyword, @Param("status") String status,
               @Param("tier") String tier, @Param("gymId") Long gymId,
               @Param("trainerIds") List<String> trainerIds,
               @Param("minDaysLeft") Integer minDaysLeft, @Param("maxDaysLeft") Integer maxDaysLeft,
               @Param("minPtRemaining") Integer minPtRemaining, @Param("minAbsentDays") Integer minAbsentDays);
    Optional<Member> findById(@Param("id") String id, @Param("gymId") Long gymId);
    boolean existsInGym(@Param("id") String id, @Param("gymId") Long gymId);
    boolean existsByNameAndPhone(@Param("name") String name, @Param("phone") String phone);
    Optional<String> findIdByNameAndPhone(@Param("name") String name, @Param("phone") String phone);
    void insertUser(Member member);
    void insertProfile(Member member);
    void insertUserGym(@Param("userId") String userId, @Param("gymId") Long gymId);
    int update(@Param("member") Member member, @Param("gymId") Long gymId);
    int delete(@Param("id") String id, @Param("gymId") Long gymId);
    int updateStatus(@Param("id") String id, @Param("isActive") int isActive, @Param("gymId") Long gymId);
    int updateTier(@Param("id") String id, @Param("tier") String tier, @Param("gymId") Long gymId);
    int updateMemberType(@Param("id") String id, @Param("memberType") String memberType, @Param("gymId") Long gymId);
    int updateRole(@Param("id") String id, @Param("role") String role, @Param("gymId") Long gymId);
    int updateAssignedTrainer(@Param("id") String id, @Param("trainerId") String trainerId, @Param("gymId") Long gymId);
    int withdraw(@Param("id") String id, @Param("gymId") Long gymId);
    void insertFreeze(@Param("memberId") String memberId, @Param("freezeStart") String freezeStart,
                      @Param("freezeEnd") String freezeEnd, @Param("reason") String reason);
    List<MemberFreeze> findFreezeByMemberId(@Param("memberId") String memberId);
    List<Membership> findMembershipsByMemberId(@Param("memberId") String memberId, @Param("gymId") Long gymId);
    Optional<Membership> findMembershipById(@Param("id") Long id, @Param("gymId") Long gymId);
    void insertMembership(@Param("m") Membership membership, @Param("gymId") Long gymId);
    void updateMembershipEndDate(@Param("id") Long id, @Param("endDate") String endDate, @Param("gymId") Long gymId);
    void updateMembershipAmounts(@Param("id") Long id, @Param("price") int price,
                                  @Param("discountAmount") int discountAmount, @Param("paidAmount") int paidAmount,
                                  @Param("paymentMethod") String paymentMethod, @Param("regType") String regType,
                                  @Param("gymId") Long gymId);
    void deleteMembership(@Param("id") Long id, @Param("gymId") Long gymId);
    void deleteMembershipsByMemberAndPackage(@Param("memberId") String memberId, @Param("packageId") Long packageId,
                                              @Param("gymId") Long gymId);
    // 양도: 원 회원의 행을 삭제 대신 상태만 변경(이력 보존)
    void markMembershipTransferred(@Param("id") Long id, @Param("gymId") Long gymId);
    // 신규/재유입/재등록 분류용 — 이 회원의 과거 이용권/PT 이력 요약({cnt, lastEndDate})
    Map<String, Object> findMembershipHistorySummary(@Param("memberId") String memberId);

    List<Membership> findExpiringMemberships(@Param("days") int days, @Param("gymId") Long gymId,
                                              @Param("offset") int offset, @Param("size") int size);
    long countExpiringMemberships(@Param("days") int days, @Param("gymId") Long gymId);

    // 회원관리 페이지 상단 집계 패널 — {total, valid, expired, expiring}
    Map<String, Object> summaryCounts(@Param("gymId") Long gymId);

    List<MemberTicket> findTickets(@Param("userId") String userId);
    Integer findTicketRemaining(@Param("userId") String userId, @Param("ticketType") String ticketType);
    // 활성 회원 전체(ONE_POINT/FEEDBACK/PHOTO/VIDEO) 잔량 합계 — {onePoint, feedback, photo, video}
    Map<String, Object> ticketTotals(@Param("gymId") Long gymId);
    void upsertTicket(@Param("userId") String userId, @Param("ticketType") String ticketType,
                      @Param("amount") int amount, @Param("gymId") Long gymId);
    void insertTicketLog(@Param("userId") String userId, @Param("ticketType") String ticketType,
                         @Param("actionType") String actionType, @Param("description") String description);

    // 티켓 지급/차감 사용내역 (구독권/티켓 관리 > 사용내역 탭)
    List<TicketLog> findTicketLogs(@Param("gymId") Long gymId, @Param("ticketType") String ticketType,
                                    @Param("keyword") String keyword,
                                    @Param("offset") int offset, @Param("size") int size);
    long countTicketLogs(@Param("gymId") Long gymId, @Param("ticketType") String ticketType,
                         @Param("keyword") String keyword);

    List<String> findAllActiveIds();

    List<PtMember> findPtMembers(@Param("lowStock") boolean lowStock,
                                  @Param("offset") int offset, @Param("size") int size,
                                  @Param("gymId") Long gymId);
    long countPtMembers(@Param("lowStock") boolean lowStock, @Param("gymId") Long gymId);

    // 실제 PT 세션 조정 — 구매분(pt_sessions_left)과 서비스/특별지급분(service_pt_sessions_left)을 분리 관리
    void adjustPtSessions(@Param("memberId") String memberId, @Param("delta") int delta, @Param("gymId") Long gymId);
    void adjustServicePtSessions(@Param("memberId") String memberId, @Param("delta") int delta, @Param("gymId") Long gymId);
}
