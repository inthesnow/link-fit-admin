package com.linkfit.admin.mapper;

import com.linkfit.admin.domain.StaffAttendance;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface StaffAttendanceMapper {
    /** 오늘 출근 현황: 전체 트레이너 + 출근 여부 */
    List<StaffAttendance> findTodayStatus(@Param("today") LocalDate today, @Param("gymId") Long gymId);

    /** 기간별 출근 기록 */
    List<StaffAttendance> findHistory(@Param("startDate") LocalDate startDate,
                                      @Param("endDate")   LocalDate endDate,
                                      @Param("userId")    String userId,
                                      @Param("gymId")     Long gymId);

    void insert(@Param("sa") StaffAttendance sa, @Param("gymId") Long gymId);

    void updateCheckOut(@Param("id") Long id, @Param("checkOut") java.time.LocalTime checkOut,
                        @Param("gymId") Long gymId);

    void delete(@Param("id") Long id, @Param("gymId") Long gymId);
}
