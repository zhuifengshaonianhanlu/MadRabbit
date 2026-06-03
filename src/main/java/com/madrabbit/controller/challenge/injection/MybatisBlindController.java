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
public class MybatisBlindController {

    @Autowired
    private ChallengeUserMapper challengeUserMapper;

    @Autowired
    private FlagService flagService;

    @GetMapping("/blind")
    @Operation(summary = "盲注关卡", description = "通过姓名搜索，只返回布尔结果，存在字符型盲注漏洞")
    public ResponseEntity<Map<String, Object>> search(@RequestParam(defaultValue = "") String name) {
        Map<String, Object> result = new HashMap<>();

        // 构造SQL预览（教学展示）
        String sqlPreview = "SELECT * FROM challenge_users WHERE name = '" + name + "'";
        result.put("sql", sqlPreview);

        // 调用Mapper执行真实查询
        try {
            List<ChallengeUser> data = challengeUserMapper.searchByNameBlind(name);
            boolean exists = data != null && !data.isEmpty();
            result.put("exists", exists);
            result.put("message", exists ? "用户存在" : "用户不存在");

            // 盲注特征检测
            if (containsBlindInjection(name)) {
                result.put("injection_detected", true);
                String flag = flagService.getFlag("injection", "level9");
                result.put("flag", flag);
            }
        } catch (Exception e) {
            boolean exists = false;
            result.put("exists", exists);
            result.put("message", "用户不存在");

            // 盲注特征检测
            if (containsBlindInjection(name)) {
                result.put("injection_detected", true);
                String flag = flagService.getFlag("injection", "level9");
                result.put("flag", flag);
            }
        }

        return ResponseEntity.ok(result);
    }

    /**
     * 盲注特征检测
     * 只检测盲注特有的payload特征，不检测通用SQL注入关键词
     */
    private boolean containsBlindInjection(String input) {
        if (input == null) return false;
        String lower = input.toLowerCase().replaceAll("\\s+", " ");

        // 布尔盲注特征
        boolean hasBoolBlind = lower.contains("substring(") || lower.contains("substr(") ||
                lower.contains("ascii(") || lower.contains("ord(") ||
                lower.contains("if(") || lower.contains("case when") ||
                lower.contains("left(") || lower.contains("right(") || lower.contains("mid(");

        // 时间盲注特征
        boolean hasTimeBlind = lower.contains("sleep(") || lower.contains("benchmark(");

        // 报错盲注特征
        boolean hasErrorBlind = lower.contains("extractvalue(") || lower.contains("updatexml(");

        return hasBoolBlind || hasTimeBlind || hasErrorBlind;
    }
}
