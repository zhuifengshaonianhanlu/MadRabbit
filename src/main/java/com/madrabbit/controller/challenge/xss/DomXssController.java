package com.madrabbit.controller.challenge.xss;

import com.madrabbit.service.FlagService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/challenge/xss/dom")
public class DomXssController {

    @Autowired
    private FlagService flagService;

    /**
     * 获取欢迎信息 - 服务端正常返回，漏洞在前端DOM操作
     * GET /api/challenge/xss/dom/greeting?name=xxx
     * 
     * 前端漏洞：前端代码用innerHTML直接渲染URL参数中的name
     * 后端：同时检测name参数是否包含XSS payload，如有则在响应中携带flag
     */
    @GetMapping("/greeting")
    public Map<String, Object> greeting(@RequestParam(value = "name", defaultValue = "Guest") String name) {
        Map<String, Object> result = new HashMap<>();

        // 更新关卡状态为"进行中"
        try {
            Map<String, Object> status = flagService.getStatus("xss", "level3");
            if (status != null && "未开始".equals(status.get("status"))) {
                flagService.updateStatus("xss", "level3", "进行中");
            }
        } catch (Exception e) { }

        result.put("success", true);
        result.put("name", name);  // 原样返回
        result.put("greeting", "Welcome, " + name + "!");

        // 检测XSS payload - DOM型XSS的检测
        if (containsXssPayload(name)) {
            String flag = flagService.getFlag("xss", "level3");
            result.put("flag", flag);
            result.put("xss_detected", true);
        }

        return result;
    }

    private boolean containsXssPayload(String input) {
        if (input == null) return false;
        String lower = input.toLowerCase();
        return lower.contains("<script") ||
               lower.contains("javascript:") ||
               lower.contains("onerror") ||
               lower.contains("onload") ||
               lower.contains("onclick") ||
               lower.contains("onmouseover") ||
               lower.contains("<img") ||
               lower.contains("<svg") ||
               lower.contains("<iframe") ||
               lower.contains("alert(") ||
               lower.contains("prompt(") ||
               lower.contains("confirm(");
    }
}
