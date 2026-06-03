package com.madrabbit.controller.challenge.injection;

import com.madrabbit.entity.ChallengeUser;
import com.madrabbit.repository.ChallengeUserMapper;
import com.madrabbit.service.FlagService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/challenge/injection/mybatis")
@Tag(name = "MyBatis SQL注入", description = "MyBatis场景SQL注入关卡")
public class MybatisOrderController {

    @Autowired
    private ChallengeUserMapper challengeUserMapper;

    @Autowired
    private FlagService flagService;

    @GetMapping("/order")
    @Operation(summary = "ORDER BY注入关卡", description = "搜索安全(#{}预编译) + 排序不安全(${}注入)")
    public Map<String, Object> search(
            @RequestParam(defaultValue = "") String name,
            @RequestParam(defaultValue = "id") String orderColumn,
            @RequestParam(defaultValue = "ASC") String orderDir) {

        Map<String, Object> result = new HashMap<>();

        // 构造SQL预览（教学展示）
        // 搜索部分用 #{} 展示（安全）
        String searchSql = "SELECT * FROM challenge_users WHERE name LIKE CONCAT('%', #{name}, '%')";
        // 排序部分用 ${} 展示（不安全）
        String orderSql = " ORDER BY ${" + orderColumn + "} ${" + orderDir + "}";
        // 实际执行的SQL预览
        String actualSql = "SELECT * FROM challenge_users WHERE name LIKE '%" + name + "%' ORDER BY " + orderColumn + " " + orderDir;

        result.put("sql_safe", searchSql);       // 安全部分的SQL模板
        result.put("sql_unsafe", orderSql);       // 不安全部分的SQL模板
        result.put("sql", actualSql);             // 实际执行的完整SQL

        try {
            List<ChallengeUser> data = challengeUserMapper.searchByNameSafeWithOrder(name, orderColumn, orderDir);
            result.put("success", true);
            result.put("data", data);

            // 注入检测 - 检查orderColumn和orderDir是否包含注入特征
            if (containsInjection(orderColumn) || containsInjection(orderDir)) {
                String flag = flagService.getFlag("injection", "level7");
                result.put("injection_detected", true);
                result.put("flag", flag);
                result.put("message", "检测到ORDER BY注入攻击！");
            } else {
                result.put("message", "查询成功");
            }
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "SQL执行错误: " + e.getMessage());

            // 即使出错，如果检测到注入也返回flag
            if (containsInjection(orderColumn) || containsInjection(orderDir)) {
                String flag = flagService.getFlag("injection", "level7");
                result.put("injection_detected", true);
                result.put("flag", flag);
            }
        }

        return result;
    }

    /**
     * ORDER BY注入检测：包含 IF、CASE、SLEEP、;、UNION、SELECT、-- 等
     */
    private boolean containsInjection(String input) {
        if (input == null) return false;
        String lower = input.toLowerCase().replaceAll("\\s+", " ");
        return lower.contains("if(") ||
               lower.contains("if (") ||
               lower.contains("case") ||
               lower.contains("sleep(") ||
               lower.contains(";") ||
               lower.contains("union") ||
               lower.contains("select") ||
               lower.contains("--") ||
               lower.contains("extractvalue") ||
               lower.contains("updatexml") ||
               lower.contains("benchmark(");
    }
}
