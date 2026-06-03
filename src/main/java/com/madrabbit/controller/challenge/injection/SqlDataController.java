package com.madrabbit.controller.challenge.injection;

import com.madrabbit.service.FlagService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.*;

/**
 * SQL注入数据泄露关卡控制器
 * 模拟通过UNION注入获取隐藏数据
 */
@RestController
@RequestMapping("/api/challenge/injection/data")
public class SqlDataController {

    @Autowired
    private FlagService flagService;

    // 模拟的公开用户数据
    private static final List<Map<String, String>> PUBLIC_USERS = new ArrayList<>();
    // 模拟的隐藏敏感数据
    private static final List<Map<String, String>> HIDDEN_DATA = new ArrayList<>();
    
    static {
        // 公开用户数据
        Map<String, String> u1 = new HashMap<>(); u1.put("id", "1"); u1.put("name", "张三"); u1.put("email", "zhangsan@example.com"); u1.put("role", "user");
        Map<String, String> u2 = new HashMap<>(); u2.put("id", "2"); u2.put("name", "李四"); u2.put("email", "lisi@example.com"); u2.put("role", "user");
        Map<String, String> u3 = new HashMap<>(); u3.put("id", "3"); u3.put("name", "王五"); u3.put("email", "wangwu@example.com"); u3.put("role", "user");
        PUBLIC_USERS.add(u1); PUBLIC_USERS.add(u2); PUBLIC_USERS.add(u3);

        // 隐藏敏感数据（正常情况下不会返回）
        Map<String, String> h1 = new HashMap<>(); h1.put("id", "100"); h1.put("name", "admin"); h1.put("email", "admin@internal.corp"); h1.put("role", "superadmin");
        Map<String, String> h2 = new HashMap<>(); h2.put("id", "101"); h2.put("name", "system"); h2.put("email", "system@internal.corp"); h2.put("role", "system");
        Map<String, String> h3 = new HashMap<>(); h3.put("id", "SECRET"); h3.put("name", "credit_cards"); h3.put("email", "4532-XXXX-XXXX-1234"); h3.put("role", "SENSITIVE");
        HIDDEN_DATA.add(h1); HIDDEN_DATA.add(h2); HIDDEN_DATA.add(h3);
    }

    /**
     * 用户搜索接口 - 存在SQL注入漏洞
     * GET /api/challenge/injection/data/search?keyword=xxx
     */
    @GetMapping("/search")
    public Map<String, Object> search(@RequestParam(required = false, defaultValue = "") String keyword) {
        Map<String, Object> result = new HashMap<>();

        // 更新关卡状态为"进行中"
        try {
            Map<String, Object> status = flagService.getStatus("injection", "level2");
            if (status != null && "未开始".equals(status.get("status"))) {
                flagService.updateStatus("injection", "level2", "进行中");
            }
        } catch (Exception e) {
            // 忽略状态更新异常
        }

        // 模拟生成的SQL语句
        String simulatedSql = "SELECT id, name, email, role FROM users WHERE name LIKE '%" + keyword + "%'";
        result.put("simulated_sql", simulatedSql);

        // 检测UNION注入
        if (containsUnionInjection(keyword)) {
            String flag = flagService.getFlag("injection", "level2");
            
            // 返回正常数据+隐藏数据
            List<Map<String, String>> allData = new ArrayList<>(PUBLIC_USERS);
            allData.addAll(HIDDEN_DATA);
            
            result.put("success", true);
            result.put("injection_detected", true);
            result.put("flag", flag);
            result.put("message", "⚠️ UNION注入成功！获取到隐藏数据。Flag: " + flag);
            result.put("results", allData);
            result.put("total", allData.size());
            return result;
        }

        // 正常搜索逻辑
        List<Map<String, String>> matchedUsers = new ArrayList<>();
        for (Map<String, String> user : PUBLIC_USERS) {
            if (keyword.isEmpty() || user.get("name").toLowerCase().contains(keyword.toLowerCase())) {
                matchedUsers.add(user);
            }
        }

        result.put("success", true);
        result.put("message", "搜索 \"" + keyword + "\" 的结果");
        result.put("results", matchedUsers);
        result.put("total", matchedUsers.size());

        return result;
    }

    /**
     * 获取提示信息
     */
    @GetMapping("/hint")
    public Map<String, Object> getHint() {
        Map<String, Object> result = new HashMap<>();
        result.put("hint", "搜索功能可能存在SQL注入漏洞，数据库中还有一张隐藏的敏感数据表...");
        result.put("table_hint", "表结构: users(id, name, email, role), secrets(id, name, email, role)");
        result.put("examples", Arrays.asList(
            "' UNION SELECT 1,2,3,4--",
            "' UNION SELECT id,name,email,role FROM secrets--",
            "' UNION ALL SELECT * FROM secrets--"
        ));
        return result;
    }

    /**
     * 检测UNION注入特征
     */
    private boolean containsUnionInjection(String input) {
        if (input == null) return false;
        String lower = input.toLowerCase().replaceAll("\\s+", " ");
        
        return lower.contains("union") ||
               lower.contains("select ") ||
               lower.contains("select*") ||
               lower.contains("' or ") ||
               lower.contains("1=1") ||
               lower.contains("information_schema") ||
               lower.contains("table_name") ||
               lower.contains("column_name") ||
               lower.contains("group_concat") ||
               lower.contains("concat(") ||
               (lower.contains("'") && lower.contains("--"));
    }
}
