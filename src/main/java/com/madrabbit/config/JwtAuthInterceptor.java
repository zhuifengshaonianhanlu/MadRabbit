package com.madrabbit.config;

import com.madrabbit.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * JWT 认证拦截器
 * 用于拦截请求并验证 JWT Token 的有效性
 */
@Component
public class JwtAuthInterceptor implements HandlerInterceptor {

    @Autowired
    private JwtUtil jwtUtil;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String path = request.getRequestURI();
        
        // 放行不需要认证的接口
        if (isPublicPath(path)) {
            return true;
        }

        // 获取 Authorization 头
        String authorization = request.getHeader("Authorization");
        
        if (authorization == null || authorization.trim().isEmpty()) {
            response.setStatus(401);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"success\":false,\"message\":\"未授权访问，请先登录\"}");
            return false;
        }

        // 提取 token (Bearer {token})
        String token = authorization;
        if (authorization.startsWith("Bearer ")) {
            token = authorization.substring(7);
        }

        // 验证 token
        if (!jwtUtil.validateToken(token)) {
            response.setStatus(401);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"success\":false,\"message\":\"Token 无效或已过期\"}");
            return false;
        }

        // 验证通过，继续处理请求
        return true;
    }

    /**
     * 判断是否为公开路径（不需要 JWT 认证）
     *
     * 免认证原则：
     * 1. 系统基础认证接口（login/register/test）
     * 2. 暴力破解关卡 —— 登录接口本身即为漏洞场景，无需前置登录
     * 3. 密码重置关卡 —— 攻击者无需登录即可重置他人密码
     * 4. JWT 令牌关卡 —— 使用自己的 Token 验证逻辑
     * 5. SQL 注入登录关卡 —— 登录绕过本身就是漏洞场景
     * 6. CSRF 关卡 —— 利用浏览器 Cookie 登录态而非 JWT
     * 7. 关卡状态查询/Flag验证 —— 前端页面核心依赖接口
     */
    private boolean isPublicPath(String path) {
        // 系统基础认证接口
        if (path.equals("/api/auth/login") ||
            path.equals("/api/auth/register") ||
            path.equals("/api/auth/test") ||
            path.equals("/api/hello")) {
            return true;
        }

        // 认证与会话安全 — 暴力破解、密码重置、JWT 关卡（漏洞场景本身无需前置登录）
        if (path.startsWith("/api/challenge/auth-session/bf/") ||
            path.startsWith("/api/challenge/auth-session/pwrest/") ||
            path.startsWith("/api/challenge/auth-session/jwt/")) {
            return true;
        }

        // SQL 注入 — 仅登录接口免认证（登录绕过是漏洞场景）
        if (path.equals("/api/challenge/injection/sql/login")) {
            return true;
        }

        // CSRF 关卡 — 利用 Cookie 登录态而非 JWT
        if (path.startsWith("/api/challenge/csrf/")) {
            return true;
        }

        // 关卡状态查询与 Flag 验证接口（前端页面核心依赖）
        if (path.equals("/api/challenge/status_get") ||
            path.equals("/api/challenge/status_update") ||
            path.equals("/api/challenge/flag_check") ||
            path.equals("/api/challenge/progress_get")) {
            return true;
        }

        return false;
    }
}
