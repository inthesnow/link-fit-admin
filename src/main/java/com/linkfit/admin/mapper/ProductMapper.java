package com.linkfit.admin.mapper;

import com.linkfit.admin.domain.Product;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

@Mapper
public interface ProductMapper {
    List<Product> findAll(@Param("type") String type, @Param("offset") int offset, @Param("size") int size,
                          @Param("gymId") Long gymId);
    long count(@Param("type") String type, @Param("gymId") Long gymId);
    Optional<Product> findById(@Param("id") Long id, @Param("gymId") Long gymId);
    // 미사용(dead code) — 아무 호출부 없음(2026-08-25 확인). gymId를 새로 요구하도록 바꾸지 않고
    // 그대로 둠 — 나중에 쓰게 되면 findAll(gymId 전용)처럼 gymId를 받게 고쳐서 쓸 것.
    List<Product> findAllActive();
    void insert(@Param("product") Product product, @Param("gymId") Long gymId);
    void update(@Param("product") Product product, @Param("gymId") Long gymId);
    void delete(@Param("id") Long id, @Param("gymId") Long gymId);
}
