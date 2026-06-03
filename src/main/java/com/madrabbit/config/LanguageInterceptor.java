package com.madrabbit.config;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.List;

/**
 * 语言拦截器 - 解析 Accept-Language Header
 */
@Component
public class LanguageInterceptor implements HandlerInterceptor {
    
    private static final List<String> SUPPORTED_LANGUAGES = 
        Arrays.asList("en-US", "zh-CN");
    
    private static final String DEFAULT_LANGUAGE = "en-US";
    
    @Override
    public boolean preHandle(HttpServletRequest request, 
                           HttpServletResponse response, 
                           Object handler) throws Exception {
        
        // 获取 Accept-Language header
        String acceptLanguage = request.getHeader("Accept-Language");
        
        // 验证并设置语言
        String language = validateLanguage(acceptLanguage);
        
        // 存入请求属性，供后续使用
        request.setAttribute("currentLanguage", language);
        
        // 存入 ThreadLocal
        LanguageContext.set(language);
        
        return true;
    }
    
    @Override
    public void afterCompletion(HttpServletRequest request, 
                              HttpServletResponse response, 
                              Object handler, 
                              Exception ex) throws Exception {
        // 清理 ThreadLocal
        LanguageContext.clear();
    }
    
    /**
     * 验证语言是否支持
     */
    private String validateLanguage(String language) {
        if (language == null || !SUPPORTED_LANGUAGES.contains(language)) {
            return DEFAULT_LANGUAGE;
        }
        return language;
    }
}
