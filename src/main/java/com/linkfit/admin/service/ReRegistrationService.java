package com.linkfit.admin.service;

import com.linkfit.admin.domain.CrmReregistrationNote;
import com.linkfit.admin.domain.ReRegistration;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface ReRegistrationService {
    List<ReRegistration> findAll(Long gymId, String status, String reason,
                                  String expiryStatus, String startDate, String endDate, int page, int size);
    long count(Long gymId, String status, String reason, String expiryStatus, String startDate, String endDate);
    Optional<ReRegistration> findById(String id, Long gymId);
    void assign(String id, String assignedTo, Long gymId);
    int autoClassify(Long gymId);         // 자동 분류 실행, 신규 생성 건수 반환
    Map<String, Integer> statusSummary(Long gymId);
    Map<String, Object> membershipSummary(Long gymId);  // 대상자(=예정자+만료)/예정자/만료 (회원권 만료일 기준)

    // 메모(스택형) — crm_member_notes와 동일한 패턴
    List<CrmReregistrationNote> findNotes(String reregistrationId, Long gymId);
    CrmReregistrationNote addNote(String reregistrationId, Long gymId, String authorId, String content);
}
