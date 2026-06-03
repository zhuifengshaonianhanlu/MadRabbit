package com.madrabbit.controller.challenge.authsession;

import com.madrabbit.entity.User;
import com.madrabbit.service.FlagService;
import com.madrabbit.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 暴力破解漏洞挑战控制器
 * 提供弱密码暴力破解的练习环境
 * 接口路径: /api/challenge/auth-session/bf/login
 */
@RestController
@RequestMapping("/api/challenge/auth-session/bf")
@Tag(name = "认证与会话安全 - 暴力破解", description = "弱密码暴力破解漏洞练习")
public class BruteForceController {

    @Autowired
    private UserService userService;
    
    @Autowired
    private FlagService flagService;

    /**
     * 暴力破解登录接口
     * POST /api/challenge/auth-session/bf/login
     * 该接口没有频率限制，允许攻击者进行暴力破解
     */
    @PostMapping("/login")
    @Operation(summary = "暴力破解登录", description = "模拟存在暴力破解漏洞的登录接口，无频率限制")
    public ResponseEntity<Map<String, Object>> bruteForceLogin(@RequestBody Map<String, String> loginRequest) {
        Map<String, Object> result = new HashMap<>();

        try {
            String username = loginRequest.get("username");
            String password = loginRequest.get("password");

            // 参数验证
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

            // 查询用户
            User user = userService.findByUsername(username);
            
            // 用户不存在
            if (user == null) {
                result.put("success", false);
                result.put("message", "The current user does not exist");
                return ResponseEntity.status(401).body(result);
            }

            // 验证密码 - 使用MD5进行对比
            // 前端传来的password已经是MD5加密后的值
            String passwordMd5 = user.getPassword_md5();
            if (passwordMd5 == null || passwordMd5.isEmpty()) {
                // 如果没有MD5密码，则使用明文密码计算MD5
                passwordMd5 = md5(user.getPassword());
            }
            
            if (!passwordMd5.equals(password)) {
                result.put("success", false);
                result.put("message", "Invalid username or password");
                return ResponseEntity.status(401).body(result);
            }

            // 检查账户状态
            if (!"ACTIVE".equals(user.getStatus())) {
                result.put("success", false);
                result.put("message", "Account is locked or disabled");
                return ResponseEntity.status(401).body(result);
            }

            // 登录成功 - 从 flags 表获取 Flag
            String flag = flagService.getFlag("auth-session", "level1");
            
            result.put("success", true);
            result.put("message", "Login successful");
            result.put("flag", flag);

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            e.printStackTrace();
            result.put("success", false);
            result.put("message", "System error, please try again later");
            return ResponseEntity.status(500).body(result);
        }
    }

    /**
     * 计算字符串的MD5值
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
