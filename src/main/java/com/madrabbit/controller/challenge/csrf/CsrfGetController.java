package com.madrabbit.controller.challenge.csrf;

import com.madrabbit.service.FlagService;
import com.madrabbit.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * GET型CSRF关卡控制器
 * 模拟无CSRF保护的邮箱修改功能
 * 需要登录态 —— CSRF利用的就是用户的登录状态
 */
@RestController
@RequestMapping("/api/challenge/csrf/get")
public class CsrfGetController {

    @Autowired
    private FlagService flagService;

    @Autowired
    private JwtUtil jwtUtil;

    // 按用户隔离存储邮箱
    private final ConcurrentHashMap<String, String> userEmails = new ConcurrentHashMap<>();

    /**
     * 从请求中获取当前用户名
     * 优先检查csrf_session cookie（CSRF攻击利用），其次检查JWT Token（正常操作兜底）
     */
    private String getCurrentUsername(HttpServletRequest request) {
        // 1. 优先检查cookie（CSRF攻击场景必须依赖cookie）
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("csrf_session".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        // 2. 兜底：检查JWT Token（正常页面操作时cookie可能未设置）
        String authorization = request.getHeader("Authorization");
        if (authorization != null && authorization.startsWith("Bearer ")) {
            try {
                String token = authorization.substring(7);
                if (jwtUtil.validateToken(token)) {
                    return jwtUtil.getUsernameFromToken(token);
                }
            } catch (Exception e) {
                // Token解析失败，忽略
            }
        }
        return null;
    }

    /**
     * 确保csrf_session cookie存在：通过JWT认证时，服务端设置cookie
     * 这样跨站请求（CSRF攻击）才能自动携带cookie
     */
    private void ensureCsrfCookie(String username, HttpServletRequest request, HttpServletResponse response) {
        boolean hasCookie = false;
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("csrf_session".equals(cookie.getName()) && username.equals(cookie.getValue())) {
                    hasCookie = true;
                    break;
                }
            }
        }
        if (!hasCookie) {
            response.setHeader("Set-Cookie", "csrf_session=" + username + "; Path=/");
        }
    }

    /**
     * 获取用户邮箱（默认为 用户名@example.com）
     */
    private String getUserEmail(String username) {
        return userEmails.getOrDefault(username, username + "@example.com");
    }

    /**
     * 获取当前邮箱
     * GET /api/challenge/csrf/get/current-email
     */
    @GetMapping("/current-email")
    public ResponseEntity<?> getCurrentEmail(HttpServletRequest request, HttpServletResponse response) {
        String username = getCurrentUsername(request);
        if (username == null) {
            Map<String, Object> err = new HashMap<>();
            err.put("success", false);
            err.put("message", "未授权访问，请先登录");
            return ResponseEntity.status(401).body(err);
        }
        ensureCsrfCookie(username, request, response);
        String email = getUserEmail(username);
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("email", email);
        result.put("original", username + "@example.com");
        return ResponseEntity.ok(result);
    }

    /**
     * 修改邮箱 - 同时支持 GET 和 POST 请求
     * POST：正常业务修改邮箱，不返回 flag
     * GET：存在 CSRF 漏洞，修改邮箱并返回 flag
     */
    @RequestMapping(value = "/change-email", method = {RequestMethod.GET, RequestMethod.POST})
    public ResponseEntity<?> changeEmail(@RequestParam String email, HttpServletRequest request, HttpServletResponse response) {
        String username = getCurrentUsername(request);
        if (username == null) {
            Map<String, Object> err = new HashMap<>();
            err.put("success", false);
            err.put("message", "未授权访问，请先登录");
            return ResponseEntity.status(401).body(err);
        }
        ensureCsrfCookie(username, request, response);

        Map<String, Object> result = new HashMap<>();

        // 更新关卡状态为"进行中"
        try {
            Map<String, Object> status = flagService.getStatus("csrf", "level1");
            if (status != null && "未开始".equals(status.get("status"))) {
                flagService.updateStatus("csrf", "level1", "进行中");
            }
        } catch (Exception e) {
            // 忽略状态更新异常
        }

        if (email == null || email.trim().isEmpty()) {
            result.put("success", false);
            result.put("message", "邮箱地址不能为空");
            return ResponseEntity.ok(result);
        }

        // 验证邮箱格式
        if (!email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            result.put("success", false);
            result.put("message", "邮箱格式无效");
            return ResponseEntity.badRequest().body(result);
        }

        // 更新邮箱
        userEmails.put(username, email);

        result.put("success", true);
        result.put("message", "邮箱修改成功");
        result.put("email", email);

        // 关键区别：只有 GET 请求才返回 flag（GET型CSRF漏洞利用）
        if ("GET".equalsIgnoreCase(request.getMethod())) {
            String flag = flagService.getFlag("csrf", "level1");
            result.put("csrf_detected", true);
            result.put("flag", flag);
            result.put("message", "检测到GET请求修改邮箱！这就是GET型CSRF的风险。Flag: " + flag);
        }

        return ResponseEntity.ok(result);
    }

    /**
     * 重置邮箱（用于重新测试）
     * POST /api/challenge/csrf/get/reset
     */
    @PostMapping("/reset")
    public ResponseEntity<?> reset(HttpServletRequest request) {
        String username = getCurrentUsername(request);
        if (username == null) {
            Map<String, Object> err = new HashMap<>();
            err.put("success", false);
            err.put("message", "未授权访问，请先登录");
            return ResponseEntity.status(401).body(err);
        }
        userEmails.remove(username);
        String email = getUserEmail(username);
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "邮箱已重置");
        result.put("email", email);
        return ResponseEntity.ok(result);
    }

    /**
     * 获取提示信息
     */
    @GetMapping("/hint")
    public ResponseEntity<?> getHint(HttpServletRequest request) {
        String username = getCurrentUsername(request);
        if (username == null) {
            Map<String, Object> err = new HashMap<>();
            err.put("success", false);
            err.put("message", "未授权访问，请先登录");
            return ResponseEntity.status(401).body(err);
        }
        Map<String, Object> result = new HashMap<>();
        result.put("tips_zh", Arrays.asList(
            "一张图片能做的事，远比你想象的多",
            "有些链接，点开不只是看看而已",
            "你的浏览器很听话，谁的话都听"
        ));
        result.put("tips_en", Arrays.asList(
            "A picture can do more than you think",
            "Some links do more than just show content",
            "Your browser is obedient - it listens to everyone"
        ));
        return ResponseEntity.ok(result);
    }
}
