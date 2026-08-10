package com.linkfit.admin.mapper;

import com.linkfit.admin.domain.Sale;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Mapper
public interface SaleMapper {
    Map<String, Object> revenueStats(@Param("date") String date, @Param("period") String period);
    List<Map<String, Object>> revenueDetail(@Param("category") String category,
                                            @Param("date") String date,
                                            @Param("period") String period);
    void insert(Sale sale);

    Optional<Sale> findById(@Param("id") Long id);

    // 환불 처리 (전액/부분 환불, 판매 기록은 삭제하지 않고 이력 보존)
    void refund(@Param("id") Long id, @Param("refundAmount") int refundAmount, @Param("reason") String reason);

    // 결제 내역 페이징
    List<Map<String, Object>> findPaged(@Param("type") String type,
                                        @Param("startDate") String startDate,
                                        @Param("endDate") String endDate,
                                        @Param("offset") int offset,
                                        @Param("size") int size);
    long countPaged(@Param("type") String type,
                    @Param("startDate") String startDate,
                    @Param("endDate") String endDate);

    // 환불 처리 (논리 삭제)
    void deleteById(@Param("id") Long id);

    // CSV export
    List<Map<String, Object>> findForExport(@Param("type") String type,
                                             @Param("startDate") String startDate,
                                             @Param("endDate") String endDate);

    // 구독(tier) 분포
    List<Map<String, Object>> tierDistribution();

    // 월별 매출 추이 (최근 N개월)
    List<Map<String, Object>> monthlyTrend(@Param("months") int months);
}
