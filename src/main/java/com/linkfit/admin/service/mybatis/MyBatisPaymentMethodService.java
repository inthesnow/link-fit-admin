package com.linkfit.admin.service.mybatis;

import com.linkfit.admin.domain.PaymentMethod;
import com.linkfit.admin.mapper.PaymentMethodMapper;
import com.linkfit.admin.service.PaymentMethodService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MyBatisPaymentMethodService implements PaymentMethodService {

    private final PaymentMethodMapper mapper;

    public MyBatisPaymentMethodService(PaymentMethodMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public List<PaymentMethod> findAll() {
        return mapper.findAll();
    }

    @Override
    public PaymentMethod add(String name) {
        PaymentMethod pm = new PaymentMethod();
        pm.setName(name.trim());
        mapper.insert(pm);
        return pm;
    }

    @Override
    public boolean existsByName(String name) {
        return mapper.existsByName(name.trim());
    }
}
