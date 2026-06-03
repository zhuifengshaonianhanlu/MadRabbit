package com.madrabbit.controller.challenge.rce;

import com.madrabbit.service.FlagService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 命令注入关卡控制器
 * 真实执行系统命令，让学习者体验命令注入漏洞
 */
@RestController
@RequestMapping("/api/challenge/rce/cmd")
public class CmdInjectController {

    @Autowired
    private FlagService flagService;

    /**
     * Ping接口 - 存在命令注入漏洞
     * POST /api/challenge/rce/cmd/ping
     * Body: { "host": "xxx" }
     */
    @PostMapping("/ping")
    @Operation(summary = "Ping命令", description = "执行Ping命令（存在命令注入漏洞）")
    public ResponseEntity<Map<String, Object>> ping(@RequestBody Map<String, String> request) {
        Map<String, Object> result = new HashMap<>();
        String host = request.get("host");

        if (host == null || host.trim().isEmpty()) {
            result.put("success", false);
            result.put("message", "请输入主机地址");
            return ResponseEntity.ok(result);
        }

        // 限制输入长度，防止超长命令
        if (host.length() > 200) {
            result.put("success", false);
            result.put("message", "输入过长");
            return ResponseEntity.ok(result);
        }

        // 更新关卡状态为"进行中"
        try {
            Map<String, Object> status = flagService.getStatus("rce", "level1");
            if (status != null && "未开始".equals(status.get("status"))) {
                flagService.updateStatus("rce", "level1", "进行中");
            }
        } catch (Exception e) {
            // 忽略状态更新异常
        }

        // 构造命令 - 故意直接拼接用户输入（这就是漏洞所在）
        String command = "ping -c 3 " + host;
        result.put("command", command);

        try {
            // 使用 /bin/sh -c 执行，允许命令注入
            ProcessBuilder pb = new ProcessBuilder("/bin/sh", "-c", command);
            pb.redirectErrorStream(true);
            Process process = pb.start();

            // 读取输出，限制最大读取量
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                int lineCount = 0;
                while ((line = reader.readLine()) != null && lineCount < 100) {
                    output.append(line).append("\n");
                    lineCount++;
                }
            }

            // 设置超时，防止长时间运行
            boolean finished = process.waitFor(10, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                output.append("\n[命令执行超时，已终止]\n");
            }

            result.put("success", true);
            result.put("output", output.toString());

            // 检测是否包含命令注入特征 - 如果用户输入了注入字符，说明成功注入
            if (containsCmdInjection(host)) {
                result.put("injection_detected", true);
                String flag = flagService.getFlag("rce", "level1");
                result.put("flag", flag);
                result.put("message", "命令注入成功！你发现了系统命令注入漏洞。");
            }

        } catch (Exception e) {
            result.put("success", true);
            result.put("output", "命令执行出错: " + e.getMessage());

            // 即使执行出错，如果输入包含注入特征也给flag
            if (containsCmdInjection(host)) {
                result.put("injection_detected", true);
                String flag = flagService.getFlag("rce", "level1");
                result.put("flag", flag);
                result.put("message", "命令注入成功！");
            }
        }

        return ResponseEntity.ok(result);
    }

    /**
     * 获取提示信息
     */
    @GetMapping("/hint")
    public Map<String, Object> getHint() {
        Map<String, Object> result = new HashMap<>();
        result.put("hint", "网络诊断工具直接将用户输入拼接到系统命令中...");
        result.put("command_hint", "命令格式: ping -c 3 {用户输入}");
        result.put("examples", Arrays.asList(
            "127.0.0.1; ls -la",
            "127.0.0.1 | cat /etc/passwd",
            "127.0.0.1 && whoami",
            "`id`",
            "$(cat /etc/passwd)"
        ));
        return result;
    }

    /**
     * 检测命令注入特征
     */
    private boolean containsCmdInjection(String input) {
        if (input == null) return false;

        // 检测常见命令注入字符和模式
        return input.contains(";") ||
               input.contains("|") ||
               input.contains("&") ||
               input.contains("`") ||
               input.contains("$(") ||
               input.contains("${") ||
               input.contains("\n") ||
               input.contains("\r") ||
               input.contains("cat ") ||
               input.contains("ls ") ||
               input.contains("ls-") ||
               input.contains("whoami") ||
               input.contains("id") ||
               input.contains("pwd") ||
               input.contains("echo ") ||
               input.contains("/etc/") ||
               input.contains("passwd") ||
               input.contains("shadow") ||
               input.contains("..") ||
               input.contains("wget ") ||
               input.contains("curl ") ||
               input.contains("nc ") ||
               input.contains("bash") ||
               input.contains("/bin/");
    }

    /**
     * 简单验证主机地址格式
     */
    private boolean isValidHost(String host) {
        if (host == null || host.isEmpty()) return false;
        // 允许IP地址或域名
        return host.matches("^[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}$") ||
               host.matches("^[a-zA-Z0-9]([a-zA-Z0-9\\-]{0,61}[a-zA-Z0-9])?(\\.[a-zA-Z]{2,})+$") ||
               host.equals("localhost");
    }
}
