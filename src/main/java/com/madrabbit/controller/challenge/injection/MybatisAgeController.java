package com.madrabbit.controller.challenge.injection;

import com.madrabbit.entity.ChallengeUser;
import com.madrabbit.repository.ChallengeUserMapper;
import com.madrabbit.service.FlagService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/challenge/injection/mybatis")
@Tag(name = "MyBatis SQL注入", description = "MyBatis场景SQL注入关卡")
public class MybatisAgeController {

    @Autowired
    private ChallengeUserMapper challengeUserMapper;

    @Autowired
    private FlagService flagService;

    @GetMapping("/age")
    @Operation(summary = "数字型注入关卡", description = "通过年龄搜索，存在数字型SQL注入漏洞")
    public ResponseEntity<Map<String, Object>> search(@RequestParam String age) {
        Map<String, Object> result = new HashMap<>();

        // 构造SQL预览（教学展示）
        String sqlPreview = "SELECT * FROM challenge_users WHERE age = " + age;
        result.put("sql", sqlPreview);

        // 调用Mapper执行真实查询
        try {
            List<ChallengeUser> data = challengeUserMapper.searchByAge(age);
            result.put("success", true);
            result.put("data", data);

            // 注入特征检测
            if (containsInjection(age)) {
                result.put("injection_detected", true);
                String flag = flagService.getFlag("injection", "level3");
                result.put("flag", flag);
                result.put("message", "SQL注入成功！你发现了MyBatis ${}的数字型注入安全风险。");
            }
        } catch (Exception e) {
            result.put("success", false);
            result.put("error", "SQL执行错误: " + e.getMessage());

            if (containsInjection(age)) {
                result.put("injection_detected", true);
                String flag = flagService.getFlag("injection", "level3");
                result.put("flag", flag);
                result.put("message", "SQL注入成功！虽然SQL语法有误，但你已经突破了安全边界。");
            }
        }

        return ResponseEntity.ok(result);
    }

    /**
     * 数字型注入检测：包含 OR、UNION、SELECT、--、; 等关键词
     */
    private boolean containsInjection(String input) {
        if (input == null) return false;
        String lower = input.toLowerCase().replaceAll("\\s+", " ");
        return lower.contains("or ") ||
               lower.contains("or(") ||
               lower.contains("union") ||
               lower.contains("select") ||
               lower.contains("--") ||
               lower.contains(";") ||
               lower.contains("1=1") ||
               lower.contains("'") ||
               lower.contains("information_schema") ||
               lower.contains("concat(") ||
               lower.contains("group_concat");
    }
}
