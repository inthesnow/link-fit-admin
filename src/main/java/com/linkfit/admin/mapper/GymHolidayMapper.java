package com.linkfit.admin.mapper;

import com.linkfit.admin.domain.GymHoliday;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface GymHolidayMapper {
    List<GymHoliday> findAll(@Param("year") int year, @Param("gymId") Long gymId);
    void insert(@Param("holiday") GymHoliday holiday, @Param("gymId") Long gymId);
    void delete(@Param("id") Long id, @Param("gymId") Long gymId);
}
