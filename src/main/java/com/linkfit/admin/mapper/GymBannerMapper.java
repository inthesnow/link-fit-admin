package com.linkfit.admin.mapper;

import com.linkfit.admin.domain.GymBanner;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface GymBannerMapper {
    List<GymBanner> findAll(@Param("gymId") Long gymId);
    void insert(@Param("banner") GymBanner banner, @Param("gymId") Long gymId);
    void delete(@Param("id") Long id, @Param("gymId") Long gymId);
    void updateSortOrder(@Param("id") Long id, @Param("sortOrder") int sortOrder, @Param("gymId") Long gymId);
    void toggleActive(@Param("id") Long id, @Param("isActive") boolean isActive, @Param("gymId") Long gymId);
}
