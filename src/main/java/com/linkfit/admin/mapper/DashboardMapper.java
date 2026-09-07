package com.linkfit.admin.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface DashboardMapper {
    Map<String, Object> memberStats(@Param("gymId") Long gymId);
    Map<String, Object> memberJoinStats(@Param("date") String date, @Param("period") String period,
                                        @Param("gymId") Long gymId);
    List<Map<String, Object>> classStats(@Param("date") String date, @Param("period") String period, @Param("gymId") Long gymId);
    Map<String, Object> revenueStats(@Param("date") String date, @Param("period") String period, @Param("gymId") Long gymId);
    List<Map<String, Object>> revenueDetail(@Param("category") String category,
                                            @Param("date") String date,
                                            @Param("period") String period,
                                            @Param("gymId") Long gymId);
    Map<String, Object> attendanceStats(@Param("date") String date, @Param("period") String period,
                                        @Param("type") String type);
    Map<String, Object> frozenStats(@Param("date") String date);

    Long countAppActiveMembers(@Param("days") int days, @Param("gymId") Long gymId);
    Map<String, Object> routineComplianceStats(@Param("days") int days, @Param("gymId") Long gymId);
}
