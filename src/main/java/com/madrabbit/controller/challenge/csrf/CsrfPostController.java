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
 * POST型CSRF关卡控制器
 * 模拟无CSRF Token验证的密码修改功能
 * 需要登录态 —— CSRF利用的就是用户的登录状态
 */
@RestController
@RequestMapping("/api/challenge/csrf/post")
public class CsrfPostController {

    @Autowired
    private FlagService flagService;

    @Autowired
    private JwtUtil jwtUtil;

    // 按用户隔离存储密码和修改状态
    private final ConcurrentHashMap<String, String> userPasswords = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Boolean> userPasswordChanged = new ConcurrentHashMap<>();

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

    private Map<String, Object> unauthorizedError() {
        Map<String, Object> err = new HashMap<>();
        err.put("success", false);
        err.put("message", "未授权访问，请先登录");
        return err;
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
     * 获取当前状态
     * GET /api/challenge/csrf/post/status
     */
    @GetMapping("/status")
    public ResponseEntity<?> getStatus(HttpServletRequest request, HttpServletResponse response) {
        String username = getCurrentUsername(request);
        if (username == null) {
            return ResponseEntity.status(401).body(unauthorizedError());
        }
        ensureCsrfCookie(username, request, response);
        boolean changed = userPasswordChanged.getOrDefault(username, false);
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("username", username);
        result.put("passwordChanged", changed);
        result.put("message", changed ? "密码已被修改" : "密码未修改");
        return ResponseEntity.ok(result);
    }

    /**
     * 修改密码 - JSON请求（正常业务，不返回flag）
     * POST /api/challenge/csrf/post/change-password
     * Content-Type: application/json
     * Body: { "newPassword": "xxx" }
     */
    @PostMapping(value = "/change-password", consumes = "application/json")
    public ResponseEntity<?> changePasswordJson(@RequestBody Map<String, String> body, HttpServletRequest request, HttpServletResponse response) {
        String username = getCurrentUsername(request);
        if (username == null) {
            return ResponseEntity.status(401).body(unauthorizedError());
        }
        ensureCsrfCookie(username, request, response);

        Map<String, Object> result = new HashMap<>();
        String newPassword = body.get("newPassword");

        // 更新关卡状态为"进行中"
        try {
            Map<String, Object> status = flagService.getStatus("csrf", "level2");
            if (status != null && "未开始".equals(status.get("status"))) {
                flagService.updateStatus("csrf", "level2", "进行中");
            }
        } catch (Exception e) {
            // 忽略状态更新异常
        }

        if (newPassword == null || newPassword.trim().isEmpty()) {
            result.put("success", false);
            result.put("message", "新密码不能为空");
            return ResponseEntity.ok(result);
        }

        // 没有验证CSRF Token！直接修改密码
        userPasswords.put(username, newPassword);
        userPasswordChanged.put(username, true);

        result.put("success", true);
        result.put("message", "密码修改成功");
        result.put("passwordChanged", true);
        // JSON请求不返回flag — 这是正常业务操作，不是CSRF攻击

        return ResponseEntity.ok(result);
    }

    /**
     * 修改密码 - 表单提交（CSRF攻击途径，返回flag）
     * POST /api/challenge/csrf/post/change-password
     * Content-Type: application/x-www-form-urlencoded
     * 表单参数: newPassword=xxx
     */
    @PostMapping(value = "/change-password", consumes = "application/x-www-form-urlencoded")
    public ResponseEntity<?> changePasswordForm(@RequestParam String newPassword, HttpServletRequest request, HttpServletResponse response) {
        String username = getCurrentUsername(request);
        if (username == null) {
            return ResponseEntity.status(401).body(unauthorizedError());
        }
        ensureCsrfCookie(username, request, response);

        Map<String, Object> result = new HashMap<>();

        // 更新关卡状态为"进行中"
        try {
            Map<String, Object> status = flagService.getStatus("csrf", "level2");
            if (status != null && "未开始".equals(status.get("status"))) {
                flagService.updateStatus("csrf", "level2", "进行中");
            }
        } catch (Exception e) {
            // 忽略状态更新异常
        }

        if (newPassword == null || newPassword.trim().isEmpty()) {
            result.put("success", false);
            result.put("message", "新密码不能为空");
            return ResponseEntity.ok(result);
        }

        // CSRF攻击成功！表单提交绕过了前端验证
        userPasswords.put(username, newPassword);
        userPasswordChanged.put(username, true);

        result.put("success", true);
        result.put("passwordChanged", true);

        // 表单提交说明CSRF攻击成功，返回flag
        String flag = flagService.getFlag("csrf", "level2");
        result.put("csrf_detected", true);
        result.put("flag", flag);
        result.put("message", "⚠️ CSRF攻击成功！密码已被恶意修改。Flag: " + flag);

        return ResponseEntity.ok(result);
    }

    /**
     * 重置状态（用于重新测试）
     * POST /api/challenge/csrf/post/reset
     */
    @PostMapping("/reset")
    public ResponseEntity<?> reset(HttpServletRequest request) {
        String username = getCurrentUsername(request);
        if (username == null) {
            return ResponseEntity.status(401).body(unauthorizedError());
        }
        userPasswords.remove(username);
        userPasswordChanged.remove(username);
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "状态已重置");
        result.put("passwordChanged", false);
        return ResponseEntity.ok(result);
    }

    /**
     * 获取提示信息
     */
    @GetMapping("/hint")
    public ResponseEntity<?> getHint(HttpServletRequest request) {
        String username = getCurrentUsername(request);
        if (username == null) {
            return ResponseEntity.status(401).body(unauthorizedError());
        }
        Map<String, Object> result = new HashMap<>();
        result.put("tips_zh", Arrays.asList(
            "表单可以藏在你看不见的地方",
            "提交按钮不一定需要人来按",
            "POST请求就安全了？那可未必"
        ));
        result.put("tips_en", Arrays.asList(
            "Forms can hide where you can't see them",
            "Submit buttons don't always need a human to press",
            "POST requests are safe? Think again"
        ));
        return ResponseEntity.ok(result);
    }
}
