package com.madrabbit.controller.challenge.injection;

import com.madrabbit.service.FlagService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.*;

/**
 * SQL注入登录绕过关卡控制器
 * 模拟SQL注入漏洞，不执行真正的SQL
 */
@RestController
@RequestMapping("/api/challenge/injection/sql")
public class SqlLoginController {

    @Autowired
    private FlagService flagService;

    // 模拟的用户数据
    private static final Map<String, String> USERS = new HashMap<>();
    static {
        USERS.put("admin", "super_secret_password_2024");
        USERS.put("user1", "password123");
        USERS.put("test", "test123");
    }

    /**
     * SQL注入登录接口
     * POST /api/challenge/injection/sql/login
     * Body: { "username": "xxx", "password": "xxx" }
     */
    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody Map<String, String> request) {
        Map<String, Object> result = new HashMap<>();
        String username = request.get("username");
        String password = request.get("password");

        // 模拟生成的SQL语句（用于展示）—— 在所有分支之前构建，确保总是返回
        String simulatedSql = "SELECT * FROM users WHERE username='" + (username != null ? username : "") + "' AND password='" + (password != null ? password : "") + "'";
        result.put("simulated_sql", simulatedSql);

        if (username == null || username.trim().isEmpty()) {
            result.put("success", false);
            result.put("message", "请输入用户名");
            return result;
        }

        // 更新关卡状态为"进行中"
        try {
            Map<String, Object> status = flagService.getStatus("injection", "level1");
            if (status != null && "未开始".equals(status.get("status"))) {
                flagService.updateStatus("injection", "level1", "进行中");
            }
        } catch (Exception e) {
            // 忽略状态更新异常
        }

        // simulated_sql 已在方法开头构建并放入 result

        // 检测SQL注入特征
        if (containsSqlInjection(username) || containsSqlInjection(password)) {
            String flag = flagService.getFlag("injection", "level1");
            result.put("success", true);
            result.put("injection_detected", true);
            result.put("flag", flag);
            result.put("message", "⚠️ SQL注入成功！登录验证被绕过。Flag: " + flag);
            result.put("user", "admin");
            result.put("role", "administrator");
            return result;
        }

        // 正常登录逻辑
        String storedPassword = USERS.get(username);
        if (storedPassword != null && storedPassword.equals(password)) {
            result.put("success", true);
            result.put("message", "登录成功");
            result.put("user", username);
            result.put("role", "admin".equals(username) ? "administrator" : "user");
        } else {
            result.put("success", false);
            result.put("message", "用户名或密码错误");
        }

        return result;
    }

    /**
     * 获取提示信息
     */
    @GetMapping("/hint")
    public Map<String, Object> getHint() {
        Map<String, Object> result = new HashMap<>();
        result.put("hint", "管理员账户是admin，但你不知道密码。试试看能否绕过密码验证？");
        result.put("examples", Arrays.asList(
            "admin' --",
            "admin' OR '1'='1",
            "' OR 1=1 --"
        ));
        return result;
    }

    /**
     * 检测SQL注入特征
     */
    private boolean containsSqlInjection(String input) {
        if (input == null) return false;
        String lower = input.toLowerCase();
        
        // 检测常见SQL注入模式
        return lower.contains("' or ") ||
               lower.contains("' or'") ||
               lower.contains("'or ") ||
               lower.contains("'or'") ||
               lower.contains("\" or ") ||
               lower.contains("1=1") ||
               lower.contains("1'='1") ||
               lower.contains("'='") ||
               lower.contains("' --") ||
               lower.contains("'--") ||
               lower.contains("'; --") ||
               lower.contains("'/*") ||
               lower.contains("*/") ||
               lower.contains("union") ||
               lower.contains("select") ||
               lower.contains(" or true") ||
               lower.contains("'true") ||
               lower.contains("admin'") ||
               (lower.contains("'") && lower.contains("="));
    }
}
