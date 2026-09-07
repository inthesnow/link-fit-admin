package com.linkfit.admin.mapper;

import com.linkfit.admin.domain.GymSetting;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface GymSettingMapper {
    GymSetting find(@Param("gymId") Long gymId);
    void upsert(@Param("setting") GymSetting setting, @Param("gymId") Long gymId);
    void updateOpenStatus(@Param("isOpen") boolean isOpen, @Param("gymId") Long gymId);
}
