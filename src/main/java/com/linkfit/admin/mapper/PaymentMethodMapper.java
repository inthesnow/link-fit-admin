package com.linkfit.admin.mapper;

import com.linkfit.admin.domain.PaymentMethod;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface PaymentMethodMapper {
    List<PaymentMethod> findAll();
    boolean existsByName(@Param("name") String name);
    void insert(PaymentMethod paymentMethod);
}
