package com.madrabbit.controller.challenge.xss;

import com.madrabbit.service.FlagService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.text.SimpleDateFormat;

@RestController
@RequestMapping("/api/challenge/xss/stored")
public class StoredXssController {

    @Autowired
    private FlagService flagService;

    // 使用内存存储留言（模拟数据库），线程安全
    private static final List<Map<String, String>> comments = new CopyOnWriteArrayList<>();

    // 初始化一些示例留言
    static {
        Map<String, String> c1 = new HashMap<>();
        c1.put("author", "Alice"); c1.put("content", "Great website! Love the design."); c1.put("time", "2026-03-15 10:30:00");
        Map<String, String> c2 = new HashMap<>();
        c2.put("author", "Bob"); c2.put("content", "Very helpful learning platform."); c2.put("time", "2026-03-16 14:20:00");
        Map<String, String> c3 = new HashMap<>();
        c3.put("author", "Charlie"); c3.put("content", "Looking forward to more challenges!"); c3.put("time", "2026-03-17 09:15:00");
        comments.add(c1); comments.add(c2); comments.add(c3);
    }

    /**
     * 获取所有留言 - 不做任何过滤/转义
     * GET /api/challenge/xss/stored/comments
     */
    @GetMapping("/comments")
    public Map<String, Object> getComments() {
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("comments", comments);
        return result;
    }

    /**
     * 发表留言 - 故意不过滤用户输入，形成存储型XSS漏洞
     * POST /api/challenge/xss/stored/comment
     * Body: { "author": "用户名", "content": "留言内容" }
     *
     * 漏洞点：留言内容不做任何转义直接存储，前端渲染时使用innerHTML展示
     * 当检测到XSS payload，返回flag
     */
    @PostMapping("/comment")
    public Map<String, Object> postComment(@RequestBody Map<String, String> request) {
        Map<String, Object> result = new HashMap<>();
        String author = request.get("author");
        String content = request.get("content");

        if (author == null || author.trim().isEmpty()) {
            result.put("success", false);
            result.put("message", "请输入昵称");
            return result;
        }
        if (content == null || content.trim().isEmpty()) {
            result.put("success", false);
            result.put("message", "请输入留言内容");
            return result;
        }

        // 更新关卡状态为"进行中"
        try {
            Map<String, Object> status = flagService.getStatus("xss", "level2");
            if (status != null && "未开始".equals(status.get("status"))) {
                flagService.updateStatus("xss", "level2", "进行中");
            }
        } catch (Exception e) { }

        // 【漏洞核心】直接存储用户输入，不做任何过滤和转义
        Map<String, String> comment = new HashMap<>();
        comment.put("author", author);
        comment.put("content", content);  // 直接存储，含XSS payload
        comment.put("time", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
        comments.add(comment);

        result.put("success", true);
        result.put("message", "留言发表成功");
        result.put("comment", comment);

        // 检测XSS payload
        if (containsXssPayload(content) || containsXssPayload(author)) {
            String flag = flagService.getFlag("xss", "level2");
            result.put("flag", flag);
            result.put("xss_detected", true);
            result.put("message", "⚠️ XSS Detected! 留言内容包含脚本代码。Flag: " + flag);
        }

        return result;
    }

    /**
     * 重置留言板（方便重复练习）
     * POST /api/challenge/xss/stored/reset
     */
    @PostMapping("/reset")
    public Map<String, Object> resetComments() {
        comments.clear();
        Map<String, String> c1 = new HashMap<>();
        c1.put("author", "Alice"); c1.put("content", "Great website! Love the design."); c1.put("time", "2026-03-15 10:30:00");
        Map<String, String> c2 = new HashMap<>();
        c2.put("author", "Bob"); c2.put("content", "Very helpful learning platform."); c2.put("time", "2026-03-16 14:20:00");
        Map<String, String> c3 = new HashMap<>();
        c3.put("author", "Charlie"); c3.put("content", "Looking forward to more challenges!"); c3.put("time", "2026-03-17 09:15:00");
        comments.add(c1); comments.add(c2); comments.add(c3);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "留言板已重置");
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
               lower.contains("confirm(") ||
               lower.contains("document.cookie") ||
               lower.contains("document.location");
    }
}
