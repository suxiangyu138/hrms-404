package com.hrms404.config;

import com.hrms404.security.AuthInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 配置：注册登录/角色拦截器（静态资源与登录接口放行）
 * 静态资源禁用缓存，避免浏览器使用旧版 JS/CSS 导致前端行为异常
 *
 * <p>参数与字段上的 {@code @NonNull} 是对父接口空值契约的显式对齐：
 * {@code WebMvcConfigurer} 已声明这些参数非空，覆写方不重复声明时，
 * IDE 的空值分析会报「Missing non-null annotation」（JDT 67109781）。
 */
@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    /** 标注在类型上：让 addInterceptor(..) 处推断出的类型也是 @NonNull，与形参对齐 */
    private final @NonNull AuthInterceptor authInterceptor;

    @Override
    public void addInterceptors(@NonNull InterceptorRegistry registry) {
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
    public void addResourceHandlers(@NonNull ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/css/**", "/js/**", "/vendor/**")
                .addResourceLocations("classpath:/static/css/", "classpath:/static/js/", "classpath:/static/vendor/")
                .setCacheControl(CacheControl.noCache().mustRevalidate());
    }
}
