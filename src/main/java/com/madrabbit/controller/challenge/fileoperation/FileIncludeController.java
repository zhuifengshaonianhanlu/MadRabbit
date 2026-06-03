package com.madrabbit.controller.challenge.fileoperation;

import com.madrabbit.service.FlagService;
import javax.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 文件包含漏洞关卡控制器（Level 3）
 * 场景：文档查看器 —— 从 static/challenges/file-operation/templates/pages/ 目录加载模板文件
 * 漏洞：单次替换 "../" 为空字符串，可用 "..../" 绕过
 * 通关方式：文件上传（level1）+ 文件包含组合攻击，读取上传的图片马触发 flag
 */
@RestController
@RequestMapping("/api/challenge/file-op/include")
public class FileIncludeController {

    @Autowired
    private FlagService flagService;

    private static final String BASE_DIR_NAME = "src/main/resources/static/challenges/file-operation/templates/pages/";
    private static final List<String> AVAILABLE_TEMPLATES = Arrays.asList("home.html", "about.html", "contact.html");

    /** 预置模板的规范化路径集合，用于判断是否读取了非预期文件 */
    private Set<String> allowedPaths;

    /** 匹配 <% ... %> 标签中的可执行代码 */
    private static final Pattern CODE_PATTERN = Pattern.compile("<%(.+?)%>", Pattern.DOTALL);

    /**
     * 启动时自动创建预置模板文件
     */
    @PostConstruct
    public void initTemplateFiles() {
        String baseDir = System.getProperty("user.dir") + "/" + BASE_DIR_NAME;
        Path dirPath = Paths.get(baseDir);
        allowedPaths = new HashSet<>();
        try {
            if (!Files.exists(dirPath)) {
                Files.createDirectories(dirPath);
            }
            Map<String, String> templates = new LinkedHashMap<>();
            templates.put("home.html", "<h2>首页</h2><p>欢迎使用文档管理系统。</p>");
            templates.put("about.html", "<h2>关于我们</h2><p>这是一个安全培训平台。</p>");
            templates.put("contact.html", "<h2>联系方式</h2><p>Email: admin@example.com</p>");

            for (Map.Entry<String, String> entry : templates.entrySet()) {
                Path filePath = dirPath.resolve(entry.getKey());
                if (!Files.exists(filePath)) {
                    Files.write(filePath, entry.getValue().getBytes(StandardCharsets.UTF_8));
                }
                allowedPaths.add(filePath.normalize().toString());
            }
        } catch (IOException e) {
            // 模板文件初始化失败不影响启动
        }
    }

    /**
     * 加载页面模板
     * GET /api/challenge/file-op/include/page?template=xxx
     *
     * 黑名单过滤（有可绕过的缺陷）：
     * 1. 拒绝绝对路径（以 / 开头或包含 :\）
     * 2. 拒绝空字节
     * 3. 单次替换 "../" 为 ""（可用 "..../" 绕过）
     */
    @GetMapping("/page")
    public Map<String, Object> loadPage(@RequestParam(required = false) String template) {
        Map<String, Object> result = new LinkedHashMap<>();

        // 空值检查
        if (template == null || template.trim().isEmpty()) {
            result.put("success", false);
            result.put("message", "请提供模板名称");
            result.put("availableTemplates", AVAILABLE_TEMPLATES);
            return result;
        }

        // === 黑名单过滤 ===

        // 1. 拒绝绝对路径
        if (template.startsWith("/") || template.contains(":\\")) {
            result.put("success", false);
            result.put("blocked_by", "absolute_path");
            result.put("message", "不允许使用绝对路径");
            return result;
        }

        // 2. 拒绝空字节
        if (template.contains("\0")) {
            result.put("success", false);
            result.put("blocked_by", "null_byte");
            result.put("message", "检测到非法字符");
            return result;
        }

        // 3. 单次替换 "../" 为空字符串（故意留下的绕过点）
        //    输入 "....//....//....//....//....//....//....//uploads/avatars/xxx.gif"
        //    → 替换后 "../../../../../../uploads/avatars/xxx.gif"
        String filtered = template.replace("../", "");

        // === 真实文件读取 ===
        String baseDir = System.getProperty("user.dir") + "/" + BASE_DIR_NAME;
        Path targetPath = Paths.get(baseDir, filtered).normalize();

        if (!Files.exists(targetPath) || !Files.isRegularFile(targetPath)) {
            result.put("success", false);
            result.put("message", "模板文件不存在");
            return result;
        }

        try {
            String content = new String(Files.readAllBytes(targetPath), StandardCharsets.UTF_8);

            // 判断读取的文件是否在预期的模板白名单中
            boolean isOutsideAllowed = !allowedPaths.contains(targetPath.toString());

            if (isOutsideAllowed) {
                // 绕过成功 → 尝试解析并执行 <% ... %> 中的代码
                List<String> execResults = executeCodeBlocks(content);

                String flag = flagService.getFlag("file-operation", "level3");
                result.put("success", true);
                result.put("flag", flag);
                result.put("content", content);
                result.put("includedPath", filtered);

                if (!execResults.isEmpty()) {
                    result.put("message", "恭喜！文件包含成功，代码已执行！");
                    result.put("executionResults", execResults);
                } else {
                    result.put("message", "恭喜！你成功绕过过滤读取到了非预期文件！");
                }
            } else {
                // 正常返回模板内容
                result.put("success", true);
                result.put("content", content);
                result.put("template", filtered);
            }
        } catch (IOException e) {
            result.put("success", false);
            result.put("message", "文件读取失败");
        }

        return result;
    }

    /**
     * 获取可用模板列表
     * GET /api/challenge/file-op/include/templates
     */
    @GetMapping("/templates")
    public Map<String, Object> listTemplates() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("templates", AVAILABLE_TEMPLATES);
        return result;
    }

    /**
     * 解析文件内容中的 <% ... %> 代码块并执行
     * 支持格式：
     *   <%exec(command)%>  — 执行系统命令
     *   <%system(command)%> — 同上
     */
    private List<String> executeCodeBlocks(String content) {
        List<String> results = new ArrayList<>();
        Matcher matcher = CODE_PATTERN.matcher(content);

        while (matcher.find()) {
            String codeBlock = matcher.group(1).trim();
            String command = extractCommand(codeBlock);
            if (command != null && !command.isEmpty()) {
                String output = executeCommand(command);
                results.add(output);
            }
        }
        return results;
    }

    /**
     * 从代码块中提取要执行的命令
     * 支持：exec(cmd), system(cmd), Runtime.exec(cmd), Runtime.getRuntime().exec(cmd)
     */
    private String extractCommand(String codeBlock) {
        // 匹配各种命令执行模式
        Pattern cmdPattern = Pattern.compile(
            "(?:exec|system|Runtime\\.exec|Runtime\\.getRuntime\\(\\)\\.exec)\\s*\\(\\s*[\"']?(.+?)[\"']?\\s*\\)",
            Pattern.DOTALL
        );
        Matcher m = cmdPattern.matcher(codeBlock);
        if (m.find()) {
            return m.group(1).trim();
        }
        return null;
    }

    /**
     * 执行系统命令并返回输出
     */
    private String executeCommand(String command) {
        try {
            ProcessBuilder pb;
            String os = System.getProperty("os.name").toLowerCase();
            if (os.contains("win")) {
                pb = new ProcessBuilder("cmd", "/c", command);
            } else {
                pb = new ProcessBuilder("sh", "-c", command);
            }
            pb.redirectErrorStream(true);
            Process process = pb.start();

            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }
            process.waitFor();
            return output.toString().trim();
        } catch (Exception e) {
            return "执行错误: " + e.getMessage();
        }
    }
}
