package com.linkfit.admin.mapper;

import com.linkfit.admin.domain.CrmSale;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Mapper
public interface CrmSalesMapper {

    List<CrmSale> findAll(@Param("gymId") Long gymId,
                           @Param("salesType") String salesType,
                           @Param("startDate") String startDate,
                           @Param("endDate") String endDate,
                           @Param("offset") int offset, @Param("size") int size);
    long count(@Param("gymId") Long gymId,
               @Param("salesType") String salesType,
               @Param("startDate") String startDate,
               @Param("endDate") String endDate);

    void insert(CrmSale sale);
    int delete(@Param("id") String id, @Param("gymId") Long gymId);

    // 월간 합계 by 유형
    List<Map<String, Object>> monthlySummary(@Param("gymId") Long gymId,
                                              @Param("monthYear") String monthYear);

    // 목표 설정
    BigDecimal findTarget(@Param("gymId") Long gymId, @Param("monthYear") String monthYear);
    void upsertTarget(@Param("gymId") Long gymId,
                      @Param("monthYear") String monthYear,
                      @Param("target") BigDecimal target);
}
