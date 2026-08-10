package com.linkfit.admin.service;

import com.linkfit.admin.domain.PaymentMethod;

import java.util.List;

public interface PaymentMethodService {
    List<PaymentMethod> findAll();
    PaymentMethod add(String name);
    boolean existsByName(String name);
}
