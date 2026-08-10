package com.linkfit.admin.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Optional;

@Mapper
public interface UserAuthMapper {
    // 앱(이메일/비밀번호) 로그인 해시 조회 — 트레이너 CRM 로그인 시 앱 계정 비밀번호 검증용
    Optional<String> findEmailPasswordHash(@Param("userId") String userId);
}
