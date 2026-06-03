package com.madrabbit.controller.challenge.rce;

import com.madrabbit.service.FlagService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/challenge/rce/spel")
@Tag(name = "SpEL代码注入", description = "SpEL表达式注入关卡")
public class SpelInjectionController {

    @Autowired
    private FlagService flagService;

    private static final Pattern EXPRESSION_PATTERN = Pattern.compile("#\\{(.+?)\\}");

    @PostMapping("/render")
    @Operation(summary = "消息模板渲染", description = "使用SpEL解析消息模板中的表达式，存在SpEL注入漏洞")
    public ResponseEntity<Map<String, Object>> render(@RequestBody Map<String, String> body) {
        Map<String, Object> result = new HashMap<>();
        String template = body.getOrDefault("template", "");
        result.put("template", template);

        // 预置上下文变量（模拟用户信息）
        ExpressionParser parser = new SpelExpressionParser();
        StandardEvaluationContext context = new StandardEvaluationContext();
        context.setVariable("name", "Alice");
        context.setVariable("role", "普通用户");
        context.setVariable("joinDate", "2024-01-15");

        // 正则匹配 #{...} 表达式，逐个解析替换
        try {
            StringBuffer rendered = new StringBuffer();
            Matcher matcher = EXPRESSION_PATTERN.matcher(template);
            while (matcher.find()) {
                String expression = matcher.group(1);
                try {
                    // 注意：SpEL变量引用用 #name 而非 name
                    Object value = parser.parseExpression("#" + expression).getValue(context);
                    // 如果表达式不是简单变量引用（如包含T(、.等），尝试直接解析
                    matcher.appendReplacement(rendered, Matcher.quoteReplacement(String.valueOf(value)));
                } catch (Exception e1) {
                    try {
                        // 直接解析（不加#前缀），支持 T(...)等表达式
                        Object value = parser.parseExpression(expression).getValue(context);
                        matcher.appendReplacement(rendered, Matcher.quoteReplacement(String.valueOf(value)));
                    } catch (Exception e2) {
                        matcher.appendReplacement(rendered, Matcher.quoteReplacement("[解析失败: " + expression + "]"));
                    }
                }
            }
            matcher.appendTail(rendered);
            result.put("rendered", rendered.toString());
        } catch (Exception e) {
            result.put("rendered", "[模板解析错误]");
            result.put("error", e.getMessage());
        }

        // SpEL注入检测
        if (containsSpelInjection(template)) {
            result.put("injection_detected", true);
            String flag = flagService.getFlag("rce", "level2");
            result.put("flag", flag);
        }

        return ResponseEntity.ok(result);
    }

    /**
     * SpEL代码注入特征检测
     */
    private boolean containsSpelInjection(String input) {
        if (input == null) return false;
        String lower = input.toLowerCase();
        return lower.contains("t(") ||
               lower.contains("getruntime") ||
               lower.contains("getclass(") ||
               lower.contains("forname(") ||
               lower.contains("exec(") ||
               lower.contains("getproperty") ||
               lower.contains("processbuilder") ||
               lower.contains("invoke(");
    }
}
