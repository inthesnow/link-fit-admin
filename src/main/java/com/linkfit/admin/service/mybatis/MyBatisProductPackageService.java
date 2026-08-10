package com.linkfit.admin.service.mybatis;

import com.linkfit.admin.domain.ProductPackage;
import com.linkfit.admin.mapper.ProductPackageMapper;
import com.linkfit.admin.service.ProductPackageService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class MyBatisProductPackageService implements ProductPackageService {

    private final ProductPackageMapper mapper;

    public MyBatisProductPackageService(ProductPackageMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public List<ProductPackage> findAll() {
        return mapper.findAll();
    }

    @Override
    public Optional<ProductPackage> findById(Long id) {
        return mapper.findById(id);
    }

    @Override
    public ProductPackage save(ProductPackage pkg) {
        mapper.insert(pkg);
        return pkg;
    }

    @Override
    public ProductPackage update(Long id, ProductPackage pkg) {
        pkg.setId(id);
        mapper.update(pkg);
        return pkg;
    }

    @Override
    public void delete(Long id) {
        mapper.delete(id);
    }
}
