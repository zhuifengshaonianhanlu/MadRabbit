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
 * 无效CSRF Token关卡控制器
 * 模拟有CSRF Token但Token未与用户绑定的情况
 * 服务端只验证Token是否有效，不验证Token是否属于当前用户
 * 攻击者可以用自己的Token冒充其他用户发起请求
 * 需要登录态 —— CSRF利用的就是用户的登录状态
 */
@RestController
@RequestMapping("/api/challenge/csrf/token")
public class CsrfTokenController {

    @Autowired
    private FlagService flagService;

    @Autowired
    private JwtUtil jwtUtil;

    // 按用户隔离存储邮箱
    private final ConcurrentHashMap<String, String> userEmails = new ConcurrentHashMap<>();

    // token -> username 映射（验证Token是否有效）
    private final ConcurrentHashMap<String, String> validTokens = new ConcurrentHashMap<>();

    // username -> token 映射（生成/查询当前用户Token）
    private final ConcurrentHashMap<String, String> userTokens = new ConcurrentHashMap<>();

    /**
     * 从请求中获取当前用户名
     * JWT优先：正常页面操作靠JWT识别用户（避免同浏览器cookie共享问题）
     * Cookie兜底：CSRF攻击请求无JWT，靠cookie识别受害者身份
     */
    private String getCurrentUsername(HttpServletRequest request) {
        // 1. 优先检查JWT Token（正常页面操作，精确识别当前用户）
        String authorization = request.getHeader("Authorization");
        if (authorization != null && authorization.startsWith("Bearer ")) {
            try {
                String token = authorization.substring(7);
                if (jwtUtil.validateToken(token)) {
                    return jwtUtil.getUsernameFromToken(token);
                }
            } catch (Exception e) {
                // Token解析失败，继续检查cookie
            }
        }
        // 2. 兜底：检查csrf_token_session cookie（CSRF攻击场景，无JWT时靠cookie识别受害者）
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("csrf_token_session".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

    /**
     * 确保csrf_token_session cookie存在：通过JWT认证时，服务端设置cookie
     * 这样跨站请求（CSRF攻击）才能自动携带cookie识别受害者
     * 使用独立的cookie名避免与其他CSRF关卡共享
     */
    private void ensureCsrfCookie(String username, HttpServletRequest request, HttpServletResponse response) {
        boolean hasCookie = false;
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("csrf_token_session".equals(cookie.getName()) && username.equals(cookie.getValue())) {
                    hasCookie = true;
                    break;
                }
            }
        }
        if (!hasCookie) {
            response.setHeader("Set-Cookie", "csrf_token_session=" + username + "; Path=/");
        }
    }

    /**
     * 获取用户邮箱（默认为 用户名@example.com）
     */
    private String getUserEmail(String username) {
        return userEmails.getOrDefault(username, username + "@example.com");
    }

    /**
     * 为用户生成CSRF Token
     * 使用简单的随机字符串模拟Token生成
     */
    private String generateCsrfToken(String username) {
        // 如果已有Token，返回已有的
        String existing = userTokens.get(username);
        if (existing != null) {
            return existing;
        }
        // 生成新Token：32位随机十六进制字符串
        String token = UUID.randomUUID().toString().replace("-", "");
        validTokens.put(token, username);
        userTokens.put(username, token);
        return token;
    }

    /**
     * 获取当前用户的CSRF Token
     * GET /api/challenge/csrf/token/csrf-token
     */
    @GetMapping("/csrf-token")
    public ResponseEntity<?> getCsrfToken(HttpServletRequest request, HttpServletResponse response) {
        String username = getCurrentUsername(request);
        if (username == null) {
            Map<String, Object> err = new HashMap<>();
            err.put("success", false);
            err.put("message", "未授权访问，请先登录");
            return ResponseEntity.status(401).body(err);
        }
        ensureCsrfCookie(username, request, response);

        String token = generateCsrfToken(username);
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("csrf_token", token);
        return ResponseEntity.ok(result);
    }

    /**
     * 获取当前邮箱
     * GET /api/challenge/csrf/token/current-email
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
        // 同时返回当前用户的CSRF Token，方便页面展示
        String token = generateCsrfToken(username);
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("email", email);
        result.put("original", username + "@example.com");
        result.put("csrf_token", token);
        return ResponseEntity.ok(result);
    }

    /**
     * 修改邮箱 - 需要携带csrf_token参数
     * 漏洞点：只验证Token是否有效，不验证Token是否属于当前用户
     * GET / POST /api/challenge/csrf/token/change-email?email=xxx&csrf_token=xxx
     */
    @RequestMapping(value = "/change-email", method = {RequestMethod.GET, RequestMethod.POST})
    public ResponseEntity<?> changeEmail(
            @RequestParam String email,
            @RequestParam String csrf_token,
            HttpServletRequest request,
            HttpServletResponse response) {
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
            Map<String, Object> status = flagService.getStatus("csrf", "level3");
            if (status != null && "未开始".equals(status.get("status"))) {
                flagService.updateStatus("csrf", "level3", "进行中");
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

        // ===== 漏洞点：CSRF Token验证 =====
        // 只检查Token是否在有效Token列表中，不检查Token是否属于当前用户
        if (csrf_token == null || csrf_token.isEmpty()) {
            result.put("success", false);
            result.put("message", "缺少CSRF Token");
            return ResponseEntity.ok(result);
        }

        String tokenOwner = validTokens.get(csrf_token);
        if (tokenOwner == null) {
            result.put("success", false);
            result.put("message", "无效的CSRF Token");
            return ResponseEntity.ok(result);
        }

        // 漏洞：这里只验证了Token是否有效，没有验证tokenOwner是否等于username
        // 攻击者可以使用自己的有效Token来冒充其他用户操作
        // 如果Token不属于当前用户，说明有人用别人的Token发起请求
        boolean tokenMisused = !tokenOwner.equals(username);

        // 更新邮箱
        userEmails.put(username, email);

        result.put("success", true);
        result.put("message", "邮箱修改成功");
        result.put("email", email);

        // 如果Token不属于当前用户，返回flag（检测到CSRF攻击）
        if (tokenMisused) {
            String flag = flagService.getFlag("csrf", "level3");
            result.put("csrf_detected", true);
            result.put("bypass_reason", "CSRF Token未与用户绑定！攻击者使用了其他用户的Token完成了请求");
            result.put("flag", flag);
            result.put("message", "CSRF Token验证被绕过！Token未与当前用户绑定。Flag: " + flag);
        }

        return ResponseEntity.ok(result);
    }

    /**
     * 重置邮箱（用于重新测试）
     * POST /api/challenge/csrf/token/reset
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
        // 重置时不删除Token，保持Token不变
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
            "有了门禁卡就能进门，但门卫从不核对卡主是谁",
            "你的Token确实是有效的，但别人的Token也是有效的",
            "Token验证了'存在性'，却忽略了'归属权'"
        ));
        result.put("tips_en", Arrays.asList(
            "The access card opens the door, but the guard never checks who owns it",
            "Your Token is valid, but so is everyone else's",
            "Token validates 'existence' but ignores 'ownership'"
        ));
        return ResponseEntity.ok(result);
    }
}
