package com.linkfit.admin.service.mybatis;

import com.linkfit.admin.domain.Membership;
import com.linkfit.admin.domain.ReRegistration;
import com.linkfit.admin.mapper.MemberMapper;
import com.linkfit.admin.mapper.ReRegistrationMapper;
import com.linkfit.admin.service.ReRegistrationService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class MyBatisReRegistrationService implements ReRegistrationService {

    private final ReRegistrationMapper mapper;
    private final MemberMapper memberMapper;

    public MyBatisReRegistrationService(ReRegistrationMapper mapper, MemberMapper memberMapper) {
        this.mapper       = mapper;
        this.memberMapper = memberMapper;
    }

    @Override
    public List<ReRegistration> findAll(Long gymId, String status, String reason,
                                         String expiryStatus, String startDate, String endDate, int page, int size) {
        return mapper.findAll(gymId, status, reason, expiryStatus, startDate, endDate, page * size, size);
    }

    @Override
    public long count(Long gymId, String status, String reason, String expiryStatus, String startDate, String endDate) {
        return mapper.count(gymId, status, reason, expiryStatus, startDate, endDate);
    }

    @Override
    public Map<String, Object> membershipSummary(Long gymId) {
        Map<String, Object> raw = mapper.summaryByMembership(gymId);
        return Map.of(
                "target",    toInt(raw.get("target")),
                "completed", toInt(raw.get("completed")),
                "expired",   toInt(raw.get("expired"))
        );
    }

    private int toInt(Object val) {
        return (val instanceof Number n) ? n.intValue() : 0;
    }

    @Override
    public Optional<ReRegistration> findById(String id) {
        return mapper.findById(id);
    }

    @Override
    public void updateStatus(String id, String status) {
        mapper.updateStatus(id, status, null);
    }

    @Override
    public void updateMemo(String id, String memo) {
        mapper.updateMemo(id, memo);
    }

    @Override
    public void assign(String id, String assignedTo) {
        mapper.assign(id, assignedTo);
    }

    @Override
    public int autoClassify(Long gymId) {
        int created = 0;

        // 이용권 만료 30일 이내
        List<Membership> expiring = memberMapper.findExpiringMemberships(30, 0, 500);
        for (Membership m : expiring) {
            String memberId = m.getMemberId();
            if (memberId == null) continue;
            if (!mapper.existsByMemberAndReason(memberId, gymId, "membership_expiry")) {
                mapper.insert(buildRecord(memberId, gymId, "membership_expiry"));
                created++;
            }
        }

        return created;
    }

    @Override
    public Map<String, Integer> statusSummary(Long gymId) {
        return Map.of(
                "pending",     mapper.countByStatus(gymId, "pending"),
                "in_progress", mapper.countByStatus(gymId, "in_progress"),
                "success",     mapper.countByStatus(gymId, "success"),
                "failed",      mapper.countByStatus(gymId, "failed"),
                "hold",        mapper.countByStatus(gymId, "hold")
        );
    }

    private ReRegistration buildRecord(String memberId, Long gymId, String reason) {
        ReRegistration r = new ReRegistration();
        r.setId(UUID.randomUUID().toString());
        r.setMemberId(memberId);
        r.setGymId(gymId);
        r.setReason(reason);
        r.setStatus("pending");
        return r;
    }
}
