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
public class MybatisInController {

    @Autowired
    private ChallengeUserMapper challengeUserMapper;

    @Autowired
    private FlagService flagService;

    @GetMapping("/in")
    @Operation(summary = "IN条件注入关卡", description = "搜索安全(#{}预编译) + 角色筛选不安全(${}注入)")
    public Map<String, Object> search(
            @RequestParam(defaultValue = "") String name,
            @RequestParam(required = false) String roles) {

        Map<String, Object> result = new HashMap<>();

        // 构造SQL预览（教学展示）
        // 搜索部分用 #{} 展示（安全）
        String safeSql = "SELECT * FROM challenge_users WHERE name LIKE CONCAT('%', #{name}, '%')";
        // 角色筛选部分用 ${} 展示（不安全）— 仅在有角色参数时展示
        String unsafeSql = " AND role IN (${roles})";

        String actualSql;
        if (roles != null && !roles.trim().isEmpty()) {
            actualSql = "SELECT * FROM challenge_users WHERE name LIKE '%" + name + "%' AND role IN (" + roles + ")";
        } else {
            actualSql = "SELECT * FROM challenge_users WHERE name LIKE '%" + name + "%'";
        }

        result.put("sql_safe", safeSql);       // 安全部分的SQL模板
        result.put("sql_unsafe", unsafeSql);    // 不安全部分的SQL模板
        result.put("sql", actualSql);           // 实际执行的完整SQL

        try {
            List<ChallengeUser> data = challengeUserMapper.searchByNameSafeWithRoles(name, roles);
            result.put("success", true);
            result.put("data", data);

            // 注入特征检测 - 仅当有roles参数时检测（空roles为正常查询）
            if (roles != null && !roles.trim().isEmpty() && containsInjection(roles)) {
                String flag = flagService.getFlag("injection", "level8");
                result.put("injection_detected", true);
                result.put("flag", flag);
                result.put("message", "检测到IN条件注入攻击！");
            } else {
                result.put("message", "查询成功");
            }
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "SQL执行错误: " + e.getMessage());

            // 即使出错，如果检测到注入也返回flag
            if (roles != null && !roles.trim().isEmpty() && containsInjection(roles)) {
                String flag = flagService.getFlag("injection", "level8");
                result.put("injection_detected", true);
                result.put("flag", flag);
            }
        }

        return result;
    }

    /**
     * IN条件注入检测：包含 OR、UNION、SELECT、;、-- 等关键词
     * 注意：不能只检测逗号，因为正常输入"'user','admin'"也有逗号和引号
     */
    private boolean containsInjection(String input) {
        if (input == null) return false;
        String lower = input.toLowerCase().replaceAll("\\s+", " ");
        return lower.contains("union") ||
               lower.contains("select") ||
               lower.contains("or ") ||
               lower.contains("or(") ||
               lower.contains(";") ||
               lower.contains("--") ||
               lower.contains("1=1") ||
               lower.contains("information_schema") ||
               lower.contains("concat(") ||
               lower.contains("group_concat");
    }
}
