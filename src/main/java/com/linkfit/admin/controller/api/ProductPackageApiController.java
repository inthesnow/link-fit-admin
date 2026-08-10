package com.linkfit.admin.controller.api;

import com.linkfit.admin.common.ApiResponse;
import com.linkfit.admin.domain.ProductPackage;
import com.linkfit.admin.service.ProductPackageService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@RestController
@RequestMapping("/api/product-packages")
public class ProductPackageApiController {

    private static final Logger log = LoggerFactory.getLogger(ProductPackageApiController.class);

    private final ProductPackageService packageService;

    public ProductPackageApiController(ProductPackageService packageService) {
        this.packageService = packageService;
    }

    @GetMapping
    public ApiResponse<List<ProductPackage>> list() {
        log.info("[ProductPackage] GET /api/product-packages");
        return ApiResponse.ok(packageService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductPackage>> get(@PathVariable Long id) {
        log.info("[ProductPackage] GET /api/product-packages/{id} - id={}", id);
        return packageService.findById(id)
            .map(p -> ResponseEntity.ok(ApiResponse.ok(p)))
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ApiResponse<ProductPackage> create(@RequestBody ProductPackage pkg) {
        log.info("[ProductPackage] POST /api/product-packages");
        if (pkg.getName() == null || pkg.getName().isBlank()) {
            return ApiResponse.error("상품명을 입력해주세요.");
        }
        if (!hasAnyComponent(pkg)) {
            return ApiResponse.error("이용권/PT/락커/운동복 중 하나 이상 구성해야 합니다.");
        }
        return ApiResponse.ok(packageService.save(pkg));
    }

    @PutMapping("/{id}")
    public ApiResponse<ProductPackage> update(@PathVariable Long id, @RequestBody ProductPackage pkg) {
        log.info("[ProductPackage] PUT /api/product-packages/{id} - id={}", id);
        if (!hasAnyComponent(pkg)) {
            return ApiResponse.error("이용권/PT/락커/운동복 중 하나 이상 구성해야 합니다.");
        }
        return ApiResponse.ok(packageService.update(id, pkg));
    }

    private boolean hasAnyComponent(ProductPackage pkg) {
        return pkg.isMembershipEnabled() || pkg.isPtEnabled() || pkg.isLockerEnabled() || pkg.isUniformEnabled();
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        log.info("[ProductPackage] DELETE /api/product-packages/{id} - id={}", id);
        packageService.delete(id);
        return ApiResponse.ok();
    }
}
