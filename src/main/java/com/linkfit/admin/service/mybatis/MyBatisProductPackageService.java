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
    public List<ProductPackage> findAll(Long gymId) {
        return mapper.findAll(gymId);
    }

    @Override
    public Optional<ProductPackage> findById(Long id, Long gymId) {
        return mapper.findById(id, gymId);
    }

    @Override
    public ProductPackage save(ProductPackage pkg, Long gymId) {
        mapper.insert(pkg, gymId);
        return pkg;
    }

    @Override
    public ProductPackage update(Long id, ProductPackage pkg, Long gymId) {
        pkg.setId(id);
        mapper.update(pkg, gymId);
        return pkg;
    }

    @Override
    public void delete(Long id, Long gymId) {
        mapper.delete(id, gymId);
    }
}
