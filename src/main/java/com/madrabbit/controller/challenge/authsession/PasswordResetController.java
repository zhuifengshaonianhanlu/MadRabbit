package com.madrabbit.controller.challenge.authsession;

import com.madrabbit.entity.User;
import com.madrabbit.service.FlagService;
import com.madrabbit.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 任意密码重置漏洞挑战控制器
 * 提供密码重置流程中未验证账号归属关系的漏洞练习
 * 接口路径：/api/challenge/auth-session/pwrest/*
 */
@RestController
@RequestMapping("/api/challenge/auth-session/pwrest")
@Tag(name = "认证与会话安全 - 任意密码重置", description = "密码重置流程中的账号绑定验证缺失漏洞练习")
public class PasswordResetController {

    @Autowired
    private UserService userService;
    
    @Autowired
    private FlagService flagService;

    // 存储验证码，key: username+email, value: code
    private final Map<String, String> captchaStore = new ConcurrentHashMap<>();
    
    // 存储验证成功的 token，key: token, value: username
    private final Map<String, String> resetTokens = new ConcurrentHashMap<>();

    /**
     * 登录接口（模拟）
     * POST /api/challenge/auth-session/pwrest/login
     */
    @PostMapping("/login")
    @Operation(summary = "模拟登录", description = "模拟用户登录，仅验证用户名和密码")
    public ResponseEntity<Map<String, Object>> login(@RequestBody Map<String, String> loginRequest) {
        Map<String, Object> result = new HashMap<>();

        try {
            String username = loginRequest.get("username");
            String password = loginRequest.get("password");

            if (username == null || username.trim().isEmpty()) {
                result.put("success", false);
                result.put("message", "Username is required");
                return ResponseEntity.badRequest().body(result);
            }

            if (password == null || password.trim().isEmpty()) {
                result.put("success", false);
                result.put("message", "Password is required");
                return ResponseEntity.badRequest().body(result);
            }

            User user = userService.findByUsername(username);
            
            if (user == null) {
                result.put("success", false);
                result.put("message", "Invalid username or password");
                return ResponseEntity.status(401).body(result);
            }

            String passwordMd5 = user.getPassword_md5();
            if (passwordMd5 == null || passwordMd5.isEmpty()) {
                passwordMd5 = md5(user.getPassword());
            }
            
            if (!passwordMd5.equals(password)) {
                result.put("success", false);
                result.put("message", "Invalid username or password");
                return ResponseEntity.status(401).body(result);
            }

            if (!"ACTIVE".equals(user.getStatus())) {
                result.put("success", false);
                result.put("message", "Account is locked or disabled");
                return ResponseEntity.status(401).body(result);
            }

            result.put("success", true);
            result.put("message", "Login successful");

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            e.printStackTrace();
            result.put("success", false);
            result.put("message", "System error, please try again later");
            return ResponseEntity.status(500).body(result);
        }
    }

    /**
     * 发送验证码
     * GET /api/challenge/auth-session/pwrest/send_code
     */
    @GetMapping("/send_code")
    @Operation(summary = "发送验证码", description = "向指定邮箱发送验证码（模拟）")
    public ResponseEntity<Map<String, Object>> sendCode(
            @RequestParam String username,
            @RequestParam String email) {
        Map<String, Object> result = new HashMap<>();

        try {
            if (username == null || username.trim().isEmpty()) {
                result.put("success", false);
                result.put("message", "Username is required");
                return ResponseEntity.badRequest().body(result);
            }

            if (email == null || email.trim().isEmpty()) {
                result.put("success", false);
                result.put("message", "Email is required");
                return ResponseEntity.badRequest().body(result);
            }

            // 验证用户名和邮箱是否匹配
            User user = userService.findByUsername(username);
            if (user == null || !email.equals(user.getEmail())) {
                result.put("success", false);
                result.put("message", "Invalid username or email");
                return ResponseEntity.status(400).body(result);
            }

            // 生成 6 位随机验证码
            String code = generateCaptcha();
            
            // 存储验证码，key 为 username+email 的组合
            String key = username + ":" + email;
            captchaStore.put(key, code);

            result.put("success", true);
            result.put("message", "The email has been sent, please check");
            
            // 只有 lili@madrabbit.com 才返回验证码（用于模拟邮件显示）
            if (email.toLowerCase().equals("lili@madrabbit.com")) {
                result.put("code", code);
            }

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            e.printStackTrace();
            result.put("success", false);
            result.put("message", "System error, please try again later");
            return ResponseEntity.status(500).body(result);
        }
    }

    /**
     * 验证验证码
     * GET /api/challenge/auth-session/pwrest/check_code
     */
    @GetMapping("/check_code")
    @Operation(summary = "验证验证码", description = "验证用户输入的验证码是否正确")
    public ResponseEntity<Map<String, Object>> checkCode(
            @RequestParam String username,
            @RequestParam String email,
            @RequestParam String code) {
        Map<String, Object> result = new HashMap<>();

        try {
            if (username == null || username.trim().isEmpty()) {
                result.put("success", false);
                result.put("message", "Username is required");
                return ResponseEntity.badRequest().body(result);
            }

            if (email == null || email.trim().isEmpty()) {
                result.put("success", false);
                result.put("message", "Email is required");
                return ResponseEntity.badRequest().body(result);
            }

            if (code == null || code.trim().isEmpty()) {
                result.put("success", false);
                result.put("message", "Verification code is required");
                return ResponseEntity.badRequest().body(result);
            }

            // 验证验证码
            String key = username + ":" + email;
            String storedCode = captchaStore.get(key);
            
            if (storedCode == null || !storedCode.equals(code)) {
                result.put("success", false);
                result.put("message", "Invalid verification code");
                return ResponseEntity.status(400).body(result);
            }

            // 验证码正确，生成 token
            String token = generateToken();
            resetTokens.put(token, username);
            
            // 删除已使用的验证码
            captchaStore.remove(key);

            result.put("success", true);
            result.put("message", "Verification successful");
            result.put("token", token);

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            e.printStackTrace();
            result.put("success", false);
            result.put("message", "System error, please try again later");
            return ResponseEntity.status(500).body(result);
        }
    }

    /**
     * 重置密码
     * POST /api/challenge/auth-session/pwrest/pw_rest
     * 漏洞点：只验证 token，不验证 token 与 username 的绑定关系
     */
    @PostMapping("/pw_rest")
    @Operation(summary = "重置密码", description = "使用验证 token 重置密码（存在漏洞：未验证 token 与账号的绑定关系）")
    public ResponseEntity<Map<String, Object>> resetPassword(@RequestBody Map<String, String> request) {
        Map<String, Object> result = new HashMap<>();

        try {
            String username = request.get("username");
            String newPassword = request.get("newPassword");
            String token = request.get("token");

            if (username == null || username.trim().isEmpty()) {
                result.put("success", false);
                result.put("message", "Username is required");
                return ResponseEntity.badRequest().body(result);
            }

            if (newPassword == null || newPassword.trim().isEmpty()) {
                result.put("success", false);
                result.put("message", "New password is required");
                return ResponseEntity.badRequest().body(result);
            }

            if (token == null || token.trim().isEmpty()) {
                result.put("success", false);
                result.put("message", "Verification token is required");
                return ResponseEntity.badRequest().body(result);
            }

            // 验证 token 是否存在
            String tokenUsername = resetTokens.get(token);
            if (tokenUsername == null) {
                result.put("success", false);
                result.put("message", "Invalid or expired verification token");
                return ResponseEntity.status(400).body(result);
            }

            // 【漏洞点】这里只验证了 token 的有效性，没有验证 token 与提交的 username 是否匹配
            // 攻击者可以使用自己的 token，但修改 username 为他人账号来重置他人密码
            
            // 查询要重置的用户
            User user = userService.findByUsername(username);
            if (user == null) {
                result.put("success", false);
                result.put("message", "User not found");
                return ResponseEntity.status(404).body(result);
            }

            // 更新密码（同时更新明文和 MD5）
            String newPasswordMd5 = md5(newPassword);
            int updated = userService.updatePassword(username, newPassword, newPasswordMd5);
            
            if (updated <= 0) {
                result.put("success", false);
                result.put("message", "Failed to update password");
                return ResponseEntity.status(500).body(result);
            }

            // 删除已使用的 token
            resetTokens.remove(token);

            result.put("success", true);
            result.put("message", "Password reset successful");

            // 如果是 lucy 账号，返回 flag
            if ("lucy".equalsIgnoreCase(username)) {
                String flag = flagService.getFlag("auth-session", "level2");
                result.put("flag", flag);
            }

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            e.printStackTrace();
            result.put("success", false);
            result.put("message", "System error, please try again later");
            return ResponseEntity.status(500).body(result);
        }
    }

    /**
     * 生成 6 位数字验证码
     */
    private String generateCaptcha() {
        SecureRandom random = new SecureRandom();
        int code = 100000 + random.nextInt(900000);
        return String.valueOf(code);
    }

    /**
     * 生成随机 token
     */
    private String generateToken() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    /**
     * 计算 MD5
     */
    private String md5(String input) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(input.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5 algorithm not found", e);
        }
    }
}
