package com.linkfit.admin.service.mybatis;

import com.linkfit.admin.domain.Product;
import com.linkfit.admin.mapper.ProductMapper;
import com.linkfit.admin.service.ProductService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class MyBatisProductService implements ProductService {

    private final ProductMapper productMapper;

    public MyBatisProductService(ProductMapper productMapper) {
        this.productMapper = productMapper;
    }

    @Override
    public List<Product> findAll(String type, int page, int size, Long gymId) {
        return productMapper.findAll(type, page * size, size, gymId);
    }

    @Override
    public long count(String type, Long gymId) {
        return productMapper.count(type, gymId);
    }

    @Override
    public Optional<Product> findById(Long id, Long gymId) {
        return productMapper.findById(id, gymId);
    }

    @Override
    public Product save(Product product, Long gymId) {
        productMapper.insert(product, gymId);
        return product;
    }

    @Override
    public Product update(Long id, Product product, Long gymId) {
        product.setId(id);
        productMapper.update(product, gymId);
        return product;
    }

    @Override
    public void delete(Long id, Long gymId) {
        productMapper.delete(id, gymId);
    }
}
