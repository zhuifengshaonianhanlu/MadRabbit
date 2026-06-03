package com.madrabbit.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web 配置类 - 添加拦截器和CORS配置
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private LanguageInterceptor languageInterceptor;
    
    @Autowired
    private JwtAuthInterceptor jwtAuthInterceptor;
    
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 添加语言拦截器（优先注册）
        registry.addInterceptor(languageInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/login.html",
                        "/css/**",
                        "/js/**",
                        "/images/**"
                );
        
        // 添加 JWT 认证拦截器
        registry.addInterceptor(jwtAuthInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                        "/api/auth/login",
                        "/api/auth/register",
                        "/api/auth/test",
                        "/api/hello"
                );
    }
    
    /**
     * 配置 CORS 跨域支持
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }

    /**
     * 配置静态资源映射 - uploads 目录
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String uploadPath = System.getProperty("user.dir") + "/uploads/";
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + uploadPath);
    }
}
