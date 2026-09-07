package com.linkfit.admin.service;

import com.linkfit.admin.domain.Member;
import com.linkfit.admin.domain.MemberTicket;
import com.linkfit.admin.domain.Membership;
import com.linkfit.admin.domain.TicketLog;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface MemberService {
    Map<String, Object> summaryCounts(Long gymId);
    List<Member> findAll(String keyword, String status, String tier, Long gymId, List<String> trainerIds,
                          Integer minDaysLeft, Integer maxDaysLeft, Integer minPtRemaining, Integer minAbsentDays,
                          int page, int size);
    long count(String keyword, String status, String tier, Long gymId, List<String> trainerIds,
               Integer minDaysLeft, Integer maxDaysLeft, Integer minPtRemaining, Integer minAbsentDays);
    Optional<Member> findById(String id, Long gymId);
    boolean existsByNameAndPhone(String name, String phone);
    Member save(Member member, Long gymId);
    Member update(String id, Member member, Long gymId);
    void delete(String id, Long gymId);
    void updateStatus(String id, String status, Long gymId);
    void updateTier(String id, String tier, Long gymId);
    void updateMemberType(String id, String memberType, Long gymId);
    void updateRole(String id, String role, Long gymId);
    void updateAssignedTrainer(String id, String trainerId, Long gymId);
    void freeze(String id, String startDate, String endDate, Long gymId);
    void withdraw(String id, Long gymId);
    List<Membership> findMemberships(String id, Long gymId);
    void addMembership(Membership membership, Long gymId);
    List<MemberTicket> findTickets(String id);
    java.util.Map<String, Object> ticketTotals(Long gymId);
    void chargeTicket(String id, String ticketType, int amount, String description, Long gymId);
    List<TicketLog> findTicketLogs(Long gymId, String ticketType, String keyword, int page, int size);
    long countTicketLogs(Long gymId, String ticketType, String keyword);

    // 회원 간 상품 양도 (이용권/락커/운동복 — 행 단위). PT는 pool 단위라 별도 메서드 사용.
    void transferMembership(Long membershipId, String targetMemberId, Long gymId);
    // PT는 특정 구매건이 아니라 회원 전체 PT 잔여 풀(구매+서비스)을 그대로 이전한다.
    void transferPtSessions(String sourceMemberId, String targetMemberId, Long gymId);

    // 이용권/락커/운동복 회수. 금액 이전 + PT 세션 환원 + 삭제가 한 트랜잭션으로 묶여야
    // 중간에 실패해도 금액이 중복되거나 유실되지 않는다.
    void deleteMembership(Long id, Long gymId);
    void deleteMembershipsByPackage(String memberId, Long packageId, Long gymId);
}
