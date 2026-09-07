package com.linkfit.admin.service.mybatis;

import com.linkfit.admin.domain.Attendance;
import com.linkfit.admin.domain.MemberFreeze;
import com.linkfit.admin.mapper.AttendanceMapper;
import com.linkfit.admin.service.AttendanceService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class MyBatisAttendanceService implements AttendanceService {

    private final AttendanceMapper attendanceMapper;

    public MyBatisAttendanceService(AttendanceMapper attendanceMapper) {
        this.attendanceMapper = attendanceMapper;
    }

    @Override
    public List<Attendance> findAll(String date, String period, Long gymId) {
        String d = (date == null || date.isBlank()) ? LocalDate.now().toString() : date;
        return attendanceMapper.findAll(d, period, gymId);
    }

    @Override
    public Optional<Attendance> findById(Long id) {
        return attendanceMapper.findById(id);
    }

    @Override
    public Attendance checkIn(Attendance attendance, Long gymId) {
        if (attendance.getAttendDate() == null) attendance.setAttendDate(LocalDate.now());
        attendanceMapper.checkIn(attendance, gymId);
        return attendance;
    }

    @Override
    public void cancel(Long id, Long gymId) {
        attendanceMapper.cancel(id, gymId);
    }

    @Override
    public List<Attendance> findFrozen(String date, Long gymId) {
        String d = (date == null || date.isBlank()) ? LocalDate.now().toString() : date;
        List<MemberFreeze> freezes = attendanceMapper.findFrozen(d, gymId);
        return freezes.stream().map(f -> {
            Attendance a = new Attendance();
            a.setMemberId(f.getMemberId());
            a.setMemberName(f.getMemberName());
            a.setType("FREEZE");
            a.setAttendDate(f.getFreezeStart());
            return a;
        }).toList();
    }
}
