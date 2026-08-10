package com.linkfit.admin.config;

import com.linkfit.admin.security.LockedCategoryInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final LockedCategoryInterceptor lockedCategoryInterceptor;

    public WebConfig(LockedCategoryInterceptor lockedCategoryInterceptor) {
        this.lockedCategoryInterceptor = lockedCategoryInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(lockedCategoryInterceptor).addPathPatterns("/**");
    }
}
