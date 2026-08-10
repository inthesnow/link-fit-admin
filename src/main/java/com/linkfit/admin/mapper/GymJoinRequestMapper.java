package com.linkfit.admin.mapper;

import com.linkfit.admin.domain.GymJoinRequest;
import com.linkfit.admin.domain.GymJoinRequestLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

@Mapper
public interface GymJoinRequestMapper {
    List<GymJoinRequest> findAll(@Param("gymId") Long gymId, @Param("status") String status,
                                  @Param("offset") int offset, @Param("size") int size);
    long count(@Param("gymId") Long gymId, @Param("status") String status);
    Optional<GymJoinRequest> findById(@Param("id") Long id);

    int approve(@Param("id") Long id);
    int reject(@Param("id") Long id);

    void insertLog(@Param("userId") String userId, @Param("gymId") Long gymId,
                    @Param("action") String action, @Param("memo") String memo,
                    @Param("actorId") String actorId);
    List<GymJoinRequestLog> findLogs(@Param("userId") String userId, @Param("gymId") Long gymId);
}
