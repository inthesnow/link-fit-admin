package com.linkfit.admin.service;

import com.linkfit.admin.domain.Product;
import java.util.List;
import java.util.Optional;

public interface ProductService {
    List<Product> findAll(String type, int page, int size, Long gymId);
    long count(String type, Long gymId);
    Optional<Product> findById(Long id, Long gymId);
    Product save(Product product, Long gymId);
    Product update(Long id, Product product, Long gymId);
    void delete(Long id, Long gymId);
}
