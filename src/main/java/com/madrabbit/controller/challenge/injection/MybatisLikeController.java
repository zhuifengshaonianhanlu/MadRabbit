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
public class MybatisLikeController {

    @Autowired
    private ChallengeUserMapper challengeUserMapper;

    @Autowired
    private FlagService flagService;

    @GetMapping("/like")
    @Operation(summary = "LIKE注入关卡", description = "通过姓名模糊搜索，存在LIKE型SQL注入漏洞")
    public ResponseEntity<Map<String, Object>> search(@RequestParam String name) {
        Map<String, Object> result = new HashMap<>();

        // 构造SQL预览（教学展示）
        String sqlPreview = "SELECT * FROM challenge_users WHERE name LIKE '%" + name + "%'";
        result.put("sql", sqlPreview);

        // 调用Mapper执行真实查询
        try {
            List<ChallengeUser> data = challengeUserMapper.searchByNameLike(name);
            result.put("success", true);
            result.put("data", data);

            // 注入特征检测
            if (containsInjection(name)) {
                result.put("injection_detected", true);
                String flag = flagService.getFlag("injection", "level5");
                result.put("flag", flag);
                result.put("message", "SQL注入成功！你发现了MyBatis LIKE '%${}%'的注入安全风险。");
            }
        } catch (Exception e) {
            result.put("success", false);
            result.put("error", "SQL执行错误: " + e.getMessage());

            if (containsInjection(name)) {
                result.put("injection_detected", true);
                String flag = flagService.getFlag("injection", "level5");
                result.put("flag", flag);
                result.put("message", "SQL注入成功！虽然SQL语法有误，但你已经突破了安全边界。");
            }
        }

        return ResponseEntity.ok(result);
    }

    /**
     * LIKE注入检测：包含单引号且包含SQL关键词，或者包含%' 等LIKE闭合特征
     */
    private boolean containsInjection(String input) {
        if (input == null) return false;
        String lower = input.toLowerCase().replaceAll("\\s+", " ");
        boolean hasQuote = input.contains("'");
        boolean hasSqlKeyword = lower.contains("union") ||
               lower.contains("select") ||
               lower.contains("or ") ||
               lower.contains("or(") ||
               lower.contains("--") ||
               lower.contains(";") ||
               lower.contains("1=1") ||
               lower.contains("information_schema") ||
               lower.contains("concat(") ||
               lower.contains("group_concat");
        boolean hasLikeClosure = input.contains("%'") || input.contains("'%");
        return (hasQuote && hasSqlKeyword) || hasLikeClosure;
    }
}
