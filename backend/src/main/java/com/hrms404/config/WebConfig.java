package com.hrms404.config;

import com.hrms404.security.AuthInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 配置：注册登录/角色拦截器（静态资源与登录接口放行）
 * 静态资源禁用缓存，避免浏览器使用旧版 JS/CSS 导致前端行为异常
 */
@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final AuthInterceptor authInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/login",
                        "/css/**",
                        "/js/**",
                        "/vendor/**",
                        "/favicon.ico",
                        "/favicon.svg",
                        "/api/auth/login",
                        "/error"
                );
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/css/**", "/js/**", "/vendor/**")
                .addResourceLocations("classpath:/static/css/", "classpath:/static/js/", "classpath:/static/vendor/")
                .setCacheControl(CacheControl.noCache().mustRevalidate());
    }
}
