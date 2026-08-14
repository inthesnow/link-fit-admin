package com.linkfit.admin.mapper;

import com.linkfit.admin.domain.CrmAnnouncement;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

@Mapper
public interface CrmAnnouncementMapper {

    List<CrmAnnouncement> findAll(@Param("gymId") Long gymId,
                                   @Param("target") String target,
                                   @Param("offset") int offset, @Param("size") int size);
    long count(@Param("gymId") Long gymId, @Param("target") String target);

    Optional<CrmAnnouncement> findById(@Param("id") String id, @Param("gymId") Long gymId);

    void insert(CrmAnnouncement announcement);
    int markSent(@Param("id") String id, @Param("gymId") Long gymId);
    int delete(@Param("id") String id, @Param("gymId") Long gymId);
}
