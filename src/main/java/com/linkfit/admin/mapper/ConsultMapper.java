package com.linkfit.admin.mapper;

import com.linkfit.admin.domain.Consult;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Mapper
public interface ConsultMapper {
    List<Consult> findAll(@Param("type") String type, @Param("offset") int offset, @Param("size") int size,
                          @Param("gymId") Long gymId);
    long count(@Param("type") String type, @Param("gymId") Long gymId);
    Optional<Consult> findById(@Param("id") Long id, @Param("gymId") Long gymId);
    void insert(@Param("consult") Consult consult, @Param("gymId") Long gymId);
    void update(@Param("consult") Consult consult, @Param("gymId") Long gymId);
    void delete(@Param("id") Long id, @Param("gymId") Long gymId);
    Map<String, Object> countStats(@Param("date") String date, @Param("period") String period,
                                   @Param("gymId") Long gymId);
}
