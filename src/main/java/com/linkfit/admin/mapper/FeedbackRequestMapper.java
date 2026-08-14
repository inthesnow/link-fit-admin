package com.linkfit.admin.mapper;

import com.linkfit.admin.domain.FeedbackRequest;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

@Mapper
public interface FeedbackRequestMapper {

    List<FeedbackRequest> findAll(@Param("gymId") Long gymId, @Param("status") String status,
                                   @Param("trainerId") String trainerId,
                                   @Param("offset") int offset, @Param("size") int size);
    long count(@Param("gymId") Long gymId, @Param("status") String status, @Param("trainerId") String trainerId);

    Optional<FeedbackRequest> findById(@Param("id") String id, @Param("gymId") Long gymId);
    void insert(FeedbackRequest request);
    int assignTrainer(@Param("id") String id, @Param("trainerId") String trainerId, @Param("gymId") Long gymId);
    int respond(@Param("id") String id, @Param("response") String response, @Param("gymId") Long gymId);
    int updateStatus(@Param("id") String id, @Param("status") String status, @Param("gymId") Long gymId);
}
