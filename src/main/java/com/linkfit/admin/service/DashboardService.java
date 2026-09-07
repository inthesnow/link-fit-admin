package com.linkfit.admin.service;

import java.util.Map;

public interface DashboardService {
    Map<String, Object> memberStats(String date, String period, Long gymId);
    Map<String, Object> consultStats(String date, String period, Long gymId);
    Map<String, Object> classStats(String date, String period, Long gymId);
    Map<String, Object> revenueStats(String date, String period, Long gymId);
    Map<String, Object> revenueDetail(String category, String date, String period, Long gymId);
    Map<String, Object> attendanceStats(String date, String period, String type, Long gymId);
    Long appUsageCount(int days, Long gymId);
    Map<String, Object> routineComplianceStats(int days, Long gymId);
}
