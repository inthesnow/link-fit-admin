package com.linkfit.admin.service.mybatis;

import com.linkfit.admin.domain.CrmReregistrationNote;
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
        int expiring = toInt(raw.get("expiring"));
        int expired  = toInt(raw.get("expired"));
        return Map.of(
                "target",   expiring + expired,
                "expiring", expiring,
                "expired",  expired
        );
    }

    private int toInt(Object val) {
        return (val instanceof Number n) ? n.intValue() : 0;
    }

    @Override
    public Optional<ReRegistration> findById(String id, Long gymId) {
        return mapper.findById(id, gymId);
    }

    @Override
    public void assign(String id, String assignedTo, Long gymId) {
        mapper.assign(id, assignedTo, gymId);
    }

    @Override
    public List<CrmReregistrationNote> findNotes(String reregistrationId, Long gymId) {
        return mapper.findNotesByReregistrationId(reregistrationId, gymId);
    }

    @Override
    public CrmReregistrationNote addNote(String reregistrationId, Long gymId, String authorId, String content) {
        CrmReregistrationNote note = new CrmReregistrationNote();
        note.setId(UUID.randomUUID().toString());
        note.setReregistrationId(reregistrationId);
        note.setGymId(gymId);
        note.setAuthorId(authorId);
        note.setContent(content);
        mapper.insertNote(note);
        return note;
    }

    @Override
    public int autoClassify(Long gymId) {
        int created = 0;

        // 이용권 만료 30일 이내 (이 gym 소속 회원만 — 배치 작업이라 페이지네이션 없이 넉넉한 상한만 둔다)
        List<Membership> expiring = memberMapper.findExpiringMemberships(30, gymId, 0, 10000);
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
