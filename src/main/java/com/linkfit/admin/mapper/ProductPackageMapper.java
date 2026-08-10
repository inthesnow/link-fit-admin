package com.linkfit.admin.mapper;

import com.linkfit.admin.domain.ProductPackage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

@Mapper
public interface ProductPackageMapper {
    List<ProductPackage> findAll();
    Optional<ProductPackage> findById(@Param("id") Long id);
    void insert(ProductPackage pkg);
    void update(ProductPackage pkg);
    void delete(@Param("id") Long id);
}
