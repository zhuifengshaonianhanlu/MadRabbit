package com.madrabbit.controller.challenge.securityconfig;

import com.madrabbit.service.FlagService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 安全配置关卡 - Level 2: Swagger API 文档泄露
 * 场景：OpenAPI 文档在生产环境未关闭，暴露了隐藏的内部管理接口。
 *       攻击者通过真实的 /v3/api-docs 发现隐藏的 internal/user-export 接口并获取 flag。
 */
@RestController
@RequestMapping("/api/challenge/sec-config/swagger")
@Tag(name = "Internal Admin", description = "Internal management endpoints - DO NOT EXPOSE")
public class SwaggerLeakController {

    @Autowired
    private FlagService flagService;

    /**
     * 隐藏的内部接口 - 返回 flag
     * GET /api/challenge/sec-config/swagger/internal/getflag
     */
    @GetMapping("/internal/getflag")
    @Operation(summary = "Export all user data (INTERNAL ONLY - DO NOT EXPOSE)",
               description = "Internal endpoint for user data export. Contains sensitive PII.")
    public Map<String, Object> userExport() {
        String flag = flagService.getFlag("security-config", "level2");

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("warning", "INTERNAL ENDPOINT - This data should never be exposed publicly!");
        result.put("warning_zh", "内部接口 - 此数据不应公开暴露！");

        List<Map<String, Object>> users = new ArrayList<>();

        Map<String, Object> user1 = new LinkedHashMap<>();
        user1.put("id", 1);
        user1.put("username", "admin");
        user1.put("email", "admin@madrabbit.internal");
        user1.put("role", "SUPER_ADMIN");
        user1.put("password_hash", "$2a$10$xK8f3dJzN...");
        users.add(user1);

        Map<String, Object> user2 = new LinkedHashMap<>();
        user2.put("id", 2);
        user2.put("username", "operator");
        user2.put("email", "ops@madrabbit.internal");
        user2.put("role", "OPERATOR");
        user2.put("password_hash", "$2a$10$mR9sLpQ7...");
        users.add(user2);

        Map<String, Object> user3 = new LinkedHashMap<>();
        user3.put("id", 3);
        user3.put("username", "dev_test");
        user3.put("email", "dev@madrabbit.internal");
        user3.put("role", "DEVELOPER");
        user3.put("password_hash", "$2a$10$bN4kWxR1...");
        users.add(user3);

        result.put("total_users", users.size());
        result.put("exported_data", users);
        result.put("flag", flag);

        return result;
    }
}
