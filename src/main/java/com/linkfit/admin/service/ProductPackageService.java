package com.linkfit.admin.service;

import com.linkfit.admin.domain.ProductPackage;

import java.util.List;
import java.util.Optional;

public interface ProductPackageService {
    List<ProductPackage> findAll();
    Optional<ProductPackage> findById(Long id);
    ProductPackage save(ProductPackage pkg);
    ProductPackage update(Long id, ProductPackage pkg);
    void delete(Long id);
}
