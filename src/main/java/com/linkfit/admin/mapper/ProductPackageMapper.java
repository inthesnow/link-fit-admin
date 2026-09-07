package com.linkfit.admin.mapper;

import com.linkfit.admin.domain.ProductPackage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

@Mapper
public interface ProductPackageMapper {
    List<ProductPackage> findAll(@Param("gymId") Long gymId);
    Optional<ProductPackage> findById(@Param("id") Long id, @Param("gymId") Long gymId);
    void insert(@Param("pkg") ProductPackage pkg, @Param("gymId") Long gymId);
    void update(@Param("pkg") ProductPackage pkg, @Param("gymId") Long gymId);
    void delete(@Param("id") Long id, @Param("gymId") Long gymId);
}
