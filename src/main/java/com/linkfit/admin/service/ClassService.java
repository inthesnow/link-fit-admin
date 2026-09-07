package com.linkfit.admin.service;

import com.linkfit.admin.domain.ClassSession;
import java.util.List;
import java.util.Optional;

public interface ClassService {
    List<ClassSession> findAll(String type, String date, int page, int size, Long gymId);
    long count(String type, String date, Long gymId);
    Optional<ClassSession> findById(Long id, Long gymId);
    ClassSession save(ClassSession session, Long gymId);
    ClassSession update(Long id, ClassSession session, Long gymId);
    void cancel(Long id, Long gymId);
    void enroll(Long classId, String memberId, Long gymId);
    void cancelEnrollment(Long classId, String memberId, Long gymId);
}
