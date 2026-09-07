package com.linkfit.admin.service.mybatis;

import com.linkfit.admin.mapper.AttendanceMapper;
import com.linkfit.admin.mapper.ConsultMapper;
import com.linkfit.admin.mapper.DashboardMapper;
import com.linkfit.admin.service.DashboardService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class MyBatisDashboardService implements DashboardService {

    private final DashboardMapper dashboardMapper;
    private final AttendanceMapper attendanceMapper;
    private final ConsultMapper consultMapper;

    public MyBatisDashboardService(DashboardMapper dashboardMapper,
                                   AttendanceMapper attendanceMapper,
                                   ConsultMapper consultMapper) {
        this.dashboardMapper = dashboardMapper;
        this.attendanceMapper = attendanceMapper;
        this.consultMapper = consultMapper;
    }

    @Override
    public Map<String, Object> memberStats(String date, String period, Long gymId) {
        String d = defaultDate(date);
        Map<String, Object> stats = new HashMap<>(dashboardMapper.memberStats(gymId));
        Map<String, Object> join = dashboardMapper.memberJoinStats(d, period, gymId);
        stats.putAll(join);
        return stats;
    }

    @Override
    public Map<String, Object> consultStats(String date, String period, Long gymId) {
        Map<String, Object> result = consultMapper.countStats(defaultDate(date), period, gymId);
        return result != null ? result : Map.of("newConsult", 0, "existingConsult", 0);
    }

    @Override
    public Map<String, Object> classStats(String date, String period, Long gymId) {
        List<Map<String, Object>> rows = dashboardMapper.classStats(defaultDate(date), period, gymId);
        Map<String, Object> result = new HashMap<>();
        long totalCnt = 0; long totalEnrolled = 0;
        for (Map<String, Object> row : rows) {
            String cat = (String) row.get("category");
            result.put(cat, Map.of("count", row.get("cnt"), "enrolled", row.get("enrolled")));
            totalCnt += toLong(row.get("cnt"));
            totalEnrolled += toLong(row.get("enrolled"));
        }
        result.put("total", Map.of("count", totalCnt, "enrolled", totalEnrolled));
        return result;
    }

    @Override
    public Map<String, Object> revenueStats(String date, String period, Long gymId) {
        Map<String, Object> result = dashboardMapper.revenueStats(defaultDate(date), period, gymId);
        return result != null ? result : Map.of("membership", 0, "groupClass", 0, "pt", 0, "locker", 0, "items", 0, "total", 0);
    }

    @Override
    public Map<String, Object> revenueDetail(String category, String date, String period, Long gymId) {
        List<Map<String, Object>> items = dashboardMapper.revenueDetail(category, defaultDate(date), period, gymId);
        long total = items.stream().mapToLong(r -> toLong(r.get("amount"))).sum();
        return Map.of("category", category, "items", items, "total", total);
    }

    @Override
    public Map<String, Object> attendanceStats(String date, String period, String type, Long gymId) {
        String d = defaultDate(date);
        Map<String, Object> stats = new HashMap<>(attendanceMapper.countStats(d, period, type, gymId));
        Map<String, Object> frozen = attendanceMapper.countFrozen(d, gymId);
        if (frozen != null) stats.putAll(frozen);
        return stats;
    }

    @Override
    public Long appUsageCount(int days, Long gymId) {
        Long count = dashboardMapper.countAppActiveMembers(days, gymId);
        return count != null ? count : 0L;
    }

    @Override
    public Map<String, Object> routineComplianceStats(int days, Long gymId) {
        Map<String, Object> result = dashboardMapper.routineComplianceStats(days, gymId);
        return result != null ? result : Map.of("assigned", 0, "completed", 0);
    }

    private String defaultDate(String date) {
        return (date == null || date.isBlank()) ? LocalDate.now().toString() : date;
    }

    private long toLong(Object val) {
        if (val == null) return 0;
        if (val instanceof Number n) return n.longValue();
        return 0;
    }
}
