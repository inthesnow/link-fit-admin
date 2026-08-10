package com.linkfit.admin.controller.api;

import com.linkfit.admin.common.ApiResponse;
import com.linkfit.admin.domain.PaymentMethod;
import com.linkfit.admin.service.PaymentMethodService;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/payment-methods")
public class PaymentMethodApiController {

    private static final Logger log = LoggerFactory.getLogger(PaymentMethodApiController.class);

    private final PaymentMethodService paymentMethodService;

    public PaymentMethodApiController(PaymentMethodService paymentMethodService) {
        this.paymentMethodService = paymentMethodService;
    }

    @GetMapping
    public ApiResponse<List<PaymentMethod>> list() {
        log.info("[PaymentMethod] GET /api/payment-methods");
        return ApiResponse.ok(paymentMethodService.findAll());
    }

    @PostMapping
    public ApiResponse<PaymentMethod> create(@RequestBody Map<String, String> body) {
        log.info("[PaymentMethod] POST /api/payment-methods");
        String name = body.get("name") == null ? "" : body.get("name").trim();
        if (name.isEmpty()) {
            return ApiResponse.error("결제수단 이름을 입력해주세요.");
        }
        if (paymentMethodService.existsByName(name)) {
            return ApiResponse.error("이미 등록된 결제수단입니다.");
        }
        return ApiResponse.ok(paymentMethodService.add(name));
    }
}
