package com.madrabbit.controller;

import com.madrabbit.entity.User;
import com.madrabbit.service.UserService;
import com.madrabbit.util.I18nUtil;
import com.madrabbit.util.JwtUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 用户认证与授权控制器
 * 实现用户登录、注册、登出等认证功能
 */
@RestController
@RequestMapping("/api/auth")
@Tag(name = "认证授权", description = "用户认证与授权相关接口")
public class AuthController {

    @Autowired
    private UserService userService;
    
    @Autowired
    private I18nUtil i18nUtil;

    @Autowired
    private JwtUtil jwtUtil;

    /**
     * 用户登录
     */
    @PostMapping("/login")
    @Operation(summary = "用户登录", description = "用户使用用户名和密码进行登录")
    public ResponseEntity<Map<String, Object>> login(@RequestBody Map<String, String> loginRequest) {

        Map<String, Object> result = new HashMap<>();

        try {
            String username = loginRequest.get("username");
            String password = loginRequest.get("password");

            // 参数验证
            if (username == null || username.trim().isEmpty()) {
                result.put("success", false);
                result.put("message", i18nUtil.getMessage("validation.required", i18nUtil.getMessage("login.username")));
                return ResponseEntity.badRequest().body(result);
            }

            if (password == null || password.trim().isEmpty()) {
                result.put("success", false);
                result.put("message", i18nUtil.getMessage("validation.required", i18nUtil.getMessage("login.password")));
                return ResponseEntity.badRequest().body(result);
            }

            // 查询用户
            User user = userService.findByUsername(username);
            if (user == null) {
                result.put("success", false);
                result.put("message", i18nUtil.getMessage("auth.login.failed"));
                return ResponseEntity.status(401).body(result);
            }

            //验证密码
            if (!user.getPassword().equals(password)) {
                result.put("success", false);
                result.put("message", i18nUtil.getMessage("auth.login.failed"));
                return ResponseEntity.status(401).body(result);
            }

            // 检查账户状态
            if (!"ACTIVE".equals(user.getStatus())) {
                result.put("success", false);
                result.put("message", i18nUtil.getMessage("auth.login.accountLocked"));
                return ResponseEntity.status(401).body(result);
            }

            // 登录成功，生成 JWT Token
            user.setPassword(null);
            String token = jwtUtil.generateToken(username);
            
            result.put("success", true);
            result.put("message", i18nUtil.getMessage("auth.login.success"));
            result.put("user", user);
            result.put("token", token);

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            e.printStackTrace();
            result.put("success", false);
            result.put("message", i18nUtil.getMessage("errors.networkError"));
            return ResponseEntity.status(500).body(result);
        }
    }

    /**
     * 用户注册
     */
    @PostMapping("/register")
    @Operation(summary = "用户注册", description = "注册新用户")
    public ResponseEntity<Map<String, Object>> register(@RequestBody User user) {
        Map<String, Object> result = new HashMap<>();

        try {
            // 参数验证
            if (user.getUsername() == null || user.getUsername().trim().isEmpty()) {
                result.put("success", false);
                result.put("message", i18nUtil.getMessage("validation.required", i18nUtil.getMessage("login.username")));
                return ResponseEntity.badRequest().body(result);
            }

            if (user.getPassword() == null || user.getPassword().trim().isEmpty()) {
                result.put("success", false);
                result.put("message", i18nUtil.getMessage("validation.required", i18nUtil.getMessage("login.password")));
                return ResponseEntity.badRequest().body(result);
            }

            // 检查用户名是否已存在
            User existingUser = userService.findByUsername(user.getUsername());
            if (existingUser != null) {
                result.put("success", false);
                result.put("message", i18nUtil.getMessage("auth.register.usernameExists"));
                return ResponseEntity.badRequest().body(result);
            }

            // 设置默认角色
            if (user.getRole() == null || user.getRole().isEmpty()) {
                user.setRole("LEARNER"); // 默认为学习者角色
            }

            //验证邮箱格式（简单验证）
            if (user.getEmail() != null && !user.getEmail().matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
                result.put("success", false);
                result.put("message", i18nUtil.getMessage("validation.invalidFormat", i18nUtil.getMessage("login.username")));
                return ResponseEntity.badRequest().body(result);
            }

            int rowsAffected = userService.createUser(user);
            if (rowsAffected > 0) {
                // 注册成功后清除密码返回
                user.setPassword(null);
                result.put("success", true);
                result.put("message", i18nUtil.getMessage("auth.register.success"));
                result.put("user", user);
            } else {
                result.put("success", false);
                result.put("message", i18nUtil.getMessage("auth.register.failed"));
                return ResponseEntity.status(500).body(result);
            }
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", i18nUtil.getMessage("errors.networkError"));
            return ResponseEntity.status(500).body(result);
        }

        return ResponseEntity.ok(result);
    }

    /**
     * 用户登出
     */
    @PostMapping("/logout")
    @Operation(summary = "用户登出", description = "用户登出，清除会话信息")
    public ResponseEntity<Map<String, Object>> logout() {
        Map<String, Object> result = new HashMap<>();

        try {
            //这里可以添加实际的登出逻辑，比如清除token等
            result.put("success", true);
            result.put("message", "登出成功");
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "登出失败：" + e.getMessage());
        }

        return ResponseEntity.ok(result);
    }

    /**
     * 获取当前用户信息
     */
    @GetMapping("/profile")
    @Operation(summary = "获取当前用户信息", description = "获取当前已登录用户的信息")
    public ResponseEntity<Map<String, Object>> getCurrentUser() {
        Map<String, Object> result = new HashMap<>();

        try {
            //这里应该从token或session中获取当前用户信息
            //目前返回模拟数据
            result.put("success", true);
            result.put("message", "请使用登录接口获取用户信息");
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "获取用户信息失败：" + e.getMessage());
            return ResponseEntity.status(500).body(result);
        }

        return ResponseEntity.ok(result);
    }

    /**
     *测试接口
     */
    @GetMapping("/test")
    @Operation(summary = "测试接口", description = "测试接口是否正常工作")
    public ResponseEntity<Map<String, Object>> test() {
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "认证服务正常运行");
        result.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.ok(result);
    }

    /**
     * 获取用户列表（临时接口）
     */
    @GetMapping("/users")
    @Operation(summary = "获取用户列表", description = "获取所有用户")
    public ResponseEntity<Map<String, Object>> getUsers() {
        Map<String, Object> result = new HashMap<>();
        try {
            result.put("success", true);
            result.put("users", userService.findAll());
            result.put("count", userService.findAll().size());
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "获取用户列表失败：" + e.getMessage());
        }
        return ResponseEntity.ok(result);
    }
}