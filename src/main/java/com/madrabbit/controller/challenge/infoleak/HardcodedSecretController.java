package com.madrabbit.controller.challenge.infoleak;

import com.madrabbit.service.FlagService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.*;

/**
 * 敏感信息泄露关卡 - Level 2: 前端硬编码泄露 (Frontend Hardcoded Secrets)
 * 场景：JS源码中包含硬编码的管理员凭据，用户需发现并使用它们登录
 */
@RestController
@RequestMapping("/api/challenge/info-leak/hardcoded")
public class HardcodedSecretController {

    @Autowired
    private FlagService flagService;

    // 硬编码的凭据（模拟前端泄露的管理员账号）
    private static final String ADMIN_USERNAME = "admin";
    private static final String ADMIN_PASSWORD = "S3cur3@dm1n!";

    /**
     * 获取管理后台页面数据
     * GET /api/challenge/info-leak/hardcoded/page
     */
    @GetMapping("/page")
    public Map<String, Object> getPage() {
        Map<String, Object> result = new HashMap<>();

        // 更新关卡状态为"进行中"
        try {
            Map<String, Object> status = flagService.getStatus("info-leak", "level2");
            if (status != null && "未开始".equals(status.get("status"))) {
                flagService.updateStatus("info-leak", "level2", "进行中");
            }
        } catch (Exception e) {
            // 忽略状态更新异常
        }

        result.put("success", true);
        result.put("title", "SecureAdmin Management Console");
        result.put("version", "v2.0.3");
        return result;
    }

    /**
     * 验证登录凭据
     * POST /api/challenge/info-leak/hardcoded/login
     */
    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody Map<String, String> body) {
        Map<String, Object> result = new HashMap<>();

        String username = body.get("username");
        String password = body.get("password");

        if (username == null || password == null) {
            result.put("success", false);
            result.put("message", "Username and password are required");
            result.put("message_zh", "用户名和密码不能为空");
            return result;
        }

        if (ADMIN_USERNAME.equals(username) && ADMIN_PASSWORD.equals(password)) {
            // 登录成功 - 返回 flag
            String flag = flagService.getFlag("info-leak", "level2");
            result.put("success", true);
            result.put("message", "Login successful! Welcome, Administrator.");
            result.put("message_zh", "登录成功！欢迎，管理员。");
            result.put("flag", flag);
            result.put("session_token", "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.admin_session");
        } else {
            result.put("success", false);
            result.put("message", "Invalid credentials. Access denied.");
            result.put("message_zh", "凭据无效，拒绝访问。");
        }

        return result;
    }
}
