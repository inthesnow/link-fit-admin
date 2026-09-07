package com.linkfit.admin.mapper;

import com.linkfit.admin.domain.OnepointRequest;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface OnepointRequestMapper {
    List<OnepointRequest> findAll(@Param("status") String status,
                                   @Param("offset") int offset,
                                   @Param("size")   int size,
                                   @Param("gymId")  Long gymId);
    long count(@Param("status") String status, @Param("gymId") Long gymId);
    void updateStatus(@Param("id")     Long   id,
                      @Param("status") String status,
                      @Param("note")   String note,
                      @Param("gymId") Long gymId);
}
