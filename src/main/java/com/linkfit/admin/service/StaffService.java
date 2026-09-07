package com.linkfit.admin.service;

import com.linkfit.admin.domain.Staff;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface StaffService {
    List<Staff> findAll(String role, int page, int size, Long gymId);
    // 회원 상세정보 "담당 트레이너 지정" 드롭박스 전용 — 반드시 이 지점(gymId) 소속 트레이너만 반환
    List<Staff> findTrainerOptions(Long gymId);
    long count(String role, Long gymId);
    Optional<Staff> findById(String id, Long gymId);
    // 트레이너 등록 화면의 "회원 검색" 드롭박스 — 이 지점에 승인된 회원 중 이름 일부 일치 검색
    List<Staff> searchMemberCandidates(String keyword, Long gymId);
    Optional<Staff> findMemberCandidate(String id, Long gymId);
    Long findTrainerGymId(String id);
    Staff promoteToTrainer(String id, Long callerGymId, LocalDate hireDate, String workStatus, LocalDate resignationDate);
    Staff update(String id, Staff staff, Long gymId);
    void revokeTrainer(String id, Long gymId);
    void updateRole(String id, String role, Long gymId);
}
