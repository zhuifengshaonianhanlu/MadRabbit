package com.madrabbit.config;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 登录验证拦截器
 */
@Component
public class LoginInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 放行 OPTIONS 预检请求（CORS）
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        
        // 获取请求 URI
        String uri = request.getRequestURI();
        
        // 如果是 API 请求，检查 token
        if (uri.startsWith("/api/")) {
            String token = request.getHeader("Authorization");
            if (token == null || token.isEmpty()) {
                // 没有 token，返回 401
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"success\":false,\"message\":\"Please login first\",\"code\":401}");
                return false;
            }
        }
        
        // 其他情况放行（前端会自行判断是否跳转登录页）
        return true;
    }
}
