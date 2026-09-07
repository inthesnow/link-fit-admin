package com.linkfit.admin.mapper;

import com.linkfit.admin.domain.TrainerSchedule;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface TrainerScheduleMapper {
    List<TrainerSchedule> findByMonth(@Param("year") int year, @Param("month") int month, @Param("gymId") Long gymId);
    List<TrainerSchedule> findByDate(@Param("date") String date, @Param("gymId") Long gymId);
    List<TrainerSchedule> findByTrainer(@Param("trainerId") String trainerId,
                                         @Param("fromDate") String fromDate,
                                         @Param("toDate") String toDate,
                                         @Param("gymId") Long gymId);
}
