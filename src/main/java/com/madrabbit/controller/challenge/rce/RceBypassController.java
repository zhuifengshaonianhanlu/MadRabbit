package com.madrabbit.controller.challenge.rce;

import com.madrabbit.service.FlagService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * RCE 过滤绕过关卡 - 日志查看器
 * 白名单过滤：只允许 ls/cat 查看 /tmp/madx-app-logs/ 目录
 * 漏洞：未过滤换行符 \n，可作为命令分隔符绕过
 */
@RestController
@RequestMapping("/api/challenge/rce/bypass")
public class RceBypassController {

    @Autowired
    private FlagService flagService;

    private static final String LOG_DIR = "/tmp/madx-app-logs/";

    private static final List<String> BLOCKED_CHARS = Arrays.asList(
            ";", "|", "&", "`", "$(", "${", ">", "<"
    );

    @PostConstruct
    public void initLogFiles() {
        try {
            Path logDir = Paths.get(LOG_DIR);
            if (!Files.exists(logDir)) {
                Files.createDirectories(logDir);
            }

            Path accessLog = logDir.resolve("access.log");
            if (!Files.exists(accessLog)) {
                Files.write(accessLog, Arrays.asList(
                        "192.168.1.10 - - [15/May/2026:10:23:01 +0800] \"GET /index.html HTTP/1.1\" 200 3842",
                        "192.168.1.15 - - [15/May/2026:10:23:05 +0800] \"POST /api/login HTTP/1.1\" 200 156",
                        "10.0.0.33 - - [15/May/2026:10:24:12 +0800] \"GET /dashboard HTTP/1.1\" 302 0",
                        "192.168.1.10 - - [15/May/2026:10:25:30 +0800] \"GET /api/users HTTP/1.1\" 200 1024",
                        "172.16.0.5 - - [15/May/2026:10:26:44 +0800] \"GET /static/logo.png HTTP/1.1\" 200 8192"
                ));
            }

            Path errorLog = logDir.resolve("error.log");
            if (!Files.exists(errorLog)) {
                Files.write(errorLog, Arrays.asList(
                        "[2026-05-15 10:30:01] ERROR: Connection timeout to database server 10.0.0.100:3306",
                        "[2026-05-15 10:31:15] WARN: Failed login attempt from 192.168.1.99 (user: admin)",
                        "[2026-05-15 10:32:44] ERROR: NullPointerException in UserService.getProfile()",
                        "[2026-05-15 10:35:02] INFO: Scheduled task completed - log rotation"
                ));
            }

            Path readme = logDir.resolve("README.txt");
            if (!Files.exists(readme)) {
                Files.write(readme, Arrays.asList(
                        "This is a restricted file viewer.",
                        "Only authorized commands are allowed.",
                        "Contact admin@madx.local for access requests."
                ));
            }
        } catch (Exception e) {
            System.err.println("[RceBypassController] Failed to initialize log files: " + e.getMessage());
        }
    }

    /**
     * 执行命令（白名单过滤）
     * POST /api/challenge/rce/bypass/execute
     * Body: { "command": "..." }
     */
    @PostMapping("/execute")
    public Map<String, Object> execute(@RequestBody Map<String, String> request) {
        Map<String, Object> result = new HashMap<>();
        String command = request.get("command");

        if (command == null || command.trim().isEmpty()) {
            result.put("success", false);
            result.put("blocked", true);
            result.put("message", "请输入命令 / Please enter a command");
            return result;
        }

        if (command.length() > 500) {
            result.put("success", false);
            result.put("blocked", true);
            result.put("message", "命令过长 / Command too long");
            return result;
        }

        // 更新关卡状态
        try {
            Map<String, Object> status = flagService.getStatus("rce", "level3");
            if (status != null && "未开始".equals(status.get("status"))) {
                flagService.updateStatus("rce", "level3", "进行中");
            }
        } catch (Exception e) {
            // 忽略状态更新异常
        }

        // === 白名单校验 ===

        // 1. 命令必须以 "ls " 或 "cat " 开头（只检查第一行）
        String firstLine = command.split("\n")[0].trim();
        if (!firstLine.startsWith("ls ") && !firstLine.startsWith("cat ")) {
            result.put("success", false);
            result.put("blocked", true);
            result.put("message", "只允许 ls 和 cat 命令 / Only 'ls' and 'cat' commands are allowed");
            return result;
        }

        // 2. 必须包含指定路径
        if (!firstLine.contains("/tmp/madx-app-logs/")) {
            result.put("success", false);
            result.put("blocked", true);
            result.put("message", "只允许访问 /tmp/madx-app-logs/ 目录 / Only /tmp/madx-app-logs/ is accessible");
            return result;
        }

        // 3. 过滤危险字符（注意：故意不过滤换行符 \n）
        for (String blocked : BLOCKED_CHARS) {
            if (command.contains(blocked)) {
                result.put("success", false);
                result.put("blocked", true);
                result.put("message", "包含非法字符: " + blocked + " / Illegal character detected: " + blocked);
                result.put("filtered_char", blocked);
                return result;
            }
        }

        // 4. 过滤路径穿越
        if (command.contains("..")) {
            result.put("success", false);
            result.put("blocked", true);
            result.put("message", "检测到路径穿越 / Path traversal detected");
            return result;
        }

        // === 通过校验，真实执行命令 ===
        try {
            ProcessBuilder pb = new ProcessBuilder("/bin/sh", "-c", command);
            pb.redirectErrorStream(true);
            Process process = pb.start();

            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                int lineCount = 0;
                while ((line = reader.readLine()) != null && lineCount < 100) {
                    output.append(line).append("\n");
                    lineCount++;
                }
            }

            boolean finished = process.waitFor(10, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                output.append("\n[Command timed out]\n");
            }

            String outputStr = output.toString();
            result.put("success", true);
            result.put("output", outputStr);

            // 检查输出是否包含 uid= (id 命令的输出特征)
            if (outputStr.contains("uid=")) {
                String flag = flagService.getFlag("rce", "level3");
                result.put("flag", flag);
                result.put("bypass_detected", true);
                result.put("message", "过滤绕过成功！/ Filter bypass successful!");
            } else {
                // 检测是否成功注入了额外命令（换行符绕过），但没执行 id
                String[] lines = command.split("\n");
                if (lines.length > 1) {
                    String extraCmd = command.substring(command.indexOf('\n') + 1).trim();
                    if (!extraCmd.isEmpty()) {
                        result.put("bypass_hint", true);
                        result.put("hint", "你成功绕过了过滤！但需要让输出包含 uid= 才能获取 Flag。试试 id 命令？ / " +
                                "You bypassed the filter! But the output must contain uid= to get the Flag. Try the 'id' command?");
                    }
                }
            }

        } catch (Exception e) {
            result.put("success", false);
            result.put("output", "命令执行出错: " + e.getMessage());
        }

        return result;
    }

    /**
     * 获取白名单规则说明
     * GET /api/challenge/rce/bypass/allowed
     */
    @GetMapping("/allowed")
    public Map<String, Object> getAllowedRules() {
        Map<String, Object> rules = new HashMap<>();
        rules.put("allowed_commands", Arrays.asList("ls", "cat"));
        rules.put("allowed_path", "/tmp/madx-app-logs/");
        rules.put("blocked_chars", BLOCKED_CHARS);
        rules.put("blocked_patterns", Arrays.asList(".."));
        rules.put("description_zh", "只允许使用 ls 和 cat 查看 /tmp/madx-app-logs/ 目录下的文件");
        rules.put("description_en", "Only ls and cat are allowed to view files in /tmp/madx-app-logs/");
        return rules;
    }
}
