package com.linkfit.admin.service;

import com.linkfit.admin.domain.Attendance;
import java.util.List;
import java.util.Optional;

public interface AttendanceService {
    List<Attendance> findAll(String date, String period, Long gymId);
    Optional<Attendance> findById(Long id);
    Attendance checkIn(Attendance attendance, Long gymId);
    void cancel(Long id, Long gymId);
    List<Attendance> findFrozen(String date, Long gymId);
}
