package com.linkfit.admin.config;

import com.linkfit.admin.security.LockedCategoryInterceptor;
import com.linkfit.admin.security.MustChangePasswordInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final MustChangePasswordInterceptor mustChangePasswordInterceptor;
    private final LockedCategoryInterceptor lockedCategoryInterceptor;

    public WebConfig(MustChangePasswordInterceptor mustChangePasswordInterceptor,
                      LockedCategoryInterceptor lockedCategoryInterceptor) {
        this.mustChangePasswordInterceptor = mustChangePasswordInterceptor;
        this.lockedCategoryInterceptor = lockedCategoryInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 최초 로그인 강제 변경 게이트가 카테고리 잠금보다 먼저 걸려야 한다
        registry.addInterceptor(mustChangePasswordInterceptor).addPathPatterns("/**");
        registry.addInterceptor(lockedCategoryInterceptor).addPathPatterns("/**");
    }
}
