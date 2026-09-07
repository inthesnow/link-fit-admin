package com.linkfit.admin.service;

import com.linkfit.admin.domain.ProductPackage;

import java.util.List;
import java.util.Optional;

public interface ProductPackageService {
    List<ProductPackage> findAll(Long gymId);
    Optional<ProductPackage> findById(Long id, Long gymId);
    ProductPackage save(ProductPackage pkg, Long gymId);
    ProductPackage update(Long id, ProductPackage pkg, Long gymId);
    void delete(Long id, Long gymId);
}
