package com.madrabbit.controller.challenge.fileoperation;

import com.madrabbit.service.FlagService;
import javax.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * 路径遍历漏洞关卡控制器（Level 2）
 * 场景：文件下载中心 —— 从 downloads/ 目录下载文档
 * 漏洞：直接拼接用户输入的文件名到 base 路径，无任何路径过滤
 * 通关方式：使用 ../ 跳出 downloads/ 目录读取敏感文件
 */
@RestController
@RequestMapping("/api/challenge/file-op/traversal")
public class PathTraversalController {

    @Autowired
    private FlagService flagService;

    private static final String DOWNLOADS_DIR = "downloads/";
    private static final String SECRET_DIR = "secret/";
    private static final List<String> AVAILABLE_FILES = Arrays.asList("report.pdf", "manual.txt", "changelog.txt");

    /** downloads/ 目录的规范化绝对路径，用于判断是否发生目录穿越 */
    private String downloadsCanonicalPath;

    /**
     * 启动时自动创建预置文件
     */
    @PostConstruct
    public void initFiles() {
        String baseDir = System.getProperty("user.dir");
        Path downloadsPath = Paths.get(baseDir, DOWNLOADS_DIR);
        Path secretPath = Paths.get(baseDir, SECRET_DIR);

        try {
            // 创建 downloads/ 目录及预置文件
            if (!Files.exists(downloadsPath)) {
                Files.createDirectories(downloadsPath);
            }

            Map<String, String> downloadFiles = new LinkedHashMap<>();
            downloadFiles.put("report.pdf", "=== Annual Security Report 2024 ===\n\nExecutive Summary\n-----------------\nThis report covers the security posture of our organization...\nAll systems passed the compliance audit.\n\n[END OF REPORT]");
            downloadFiles.put("manual.txt", "=== System User Manual ===\n\n1. Getting Started\n   - Login to the dashboard\n   - Navigate to File Management\n\n2. File Operations\n   - Upload: Click 'Upload' button\n   - Download: Select file and click 'Download'\n\n3. Support\n   Contact: support@example.com");
            downloadFiles.put("changelog.txt", "=== Changelog ===\n\nv2.1.0 (2024-03-15)\n- Added file download feature\n- Improved UI layout\n\nv2.0.0 (2024-01-10)\n- Major system redesign\n- New authentication module\n\nv1.0.0 (2023-06-01)\n- Initial release");

            for (Map.Entry<String, String> entry : downloadFiles.entrySet()) {
                Path filePath = downloadsPath.resolve(entry.getKey());
                if (!Files.exists(filePath)) {
                    Files.write(filePath, entry.getValue().getBytes(StandardCharsets.UTF_8));
                }
            }

            // 记录 downloads/ 的规范化路径
            downloadsCanonicalPath = downloadsPath.toFile().getCanonicalPath();

            // 创建 secret/ 目录及敏感文件
            if (!Files.exists(secretPath)) {
                Files.createDirectories(secretPath);
            }

            Path credentialsFile = secretPath.resolve("credentials.txt");
            if (!Files.exists(credentialsFile)) {
                String sensitiveContent =
                    "=== CONFIDENTIAL - Internal Credentials ===\n\n" +
                    "Database:\n" +
                    "  host: 192.168.1.100\n" +
                    "  port: 3306\n" +
                    "  username: db_admin\n" +
                    "  password: S3cur3_DB_P@ss!\n\n" +
                    "Admin Panel:\n" +
                    "  url: /admin\n" +
                    "  username: administrator\n" +
                    "  password: Admin@2024#Secure\n\n" +
                    "API Keys:\n" +
                    "  payment_gateway: sk_live_4eC39HqLyjWDarjtT1zdp7dc\n" +
                    "  email_service: SG.xxxxxxxxxxxxxxxxxxxxx\n";
                Files.write(credentialsFile, sensitiveContent.getBytes(StandardCharsets.UTF_8));
            }

        } catch (IOException e) {
            System.err.println("[PathTraversalController] Failed to initialize files: " + e.getMessage());
        }
    }

    /**
     * 下载文件
     * GET /api/challenge/file-op/traversal/download?file=xxx
     *
     * 漏洞点：直接将用户输入拼接到 downloads/ 路径，无任何过滤
     */
    @GetMapping("/download")
    public Map<String, Object> downloadFile(@RequestParam(required = false) String file) {
        Map<String, Object> result = new LinkedHashMap<>();

        if (file == null || file.trim().isEmpty()) {
            result.put("success", false);
            result.put("message", "请提供文件名");
            result.put("availableFiles", AVAILABLE_FILES);
            return result;
        }

        // 【漏洞核心】直接拼接，无任何路径过滤
        String baseDir = System.getProperty("user.dir") + "/" + DOWNLOADS_DIR;
        Path targetPath = Paths.get(baseDir, file);

        // 检查文件是否存在
        if (!Files.exists(targetPath) || !Files.isRegularFile(targetPath)) {
            result.put("success", false);
            result.put("message", "文件不存在");
            return result;
        }

        try {
            String content = new String(Files.readAllBytes(targetPath), StandardCharsets.UTF_8);

            // 判断规范化后的路径是否仍在 downloads/ 目录内
            String resolvedCanonical = targetPath.toFile().getCanonicalPath();
            boolean isOutsideDownloads = !resolvedCanonical.startsWith(downloadsCanonicalPath);

            if (isOutsideDownloads) {
                // 路径遍历成功
                String flag = flagService.getFlag("file-operation", "level2");
                result.put("success", true);
                result.put("flag", flag);
                result.put("message", "恭喜！你成功利用路径遍历读取到了敏感文件！");
                result.put("content", content);
                result.put("resolvedPath", targetPath.normalize().toString());
            } else {
                // 正常返回文件内容
                result.put("success", true);
                result.put("content", content);
                result.put("filename", targetPath.getFileName().toString());
            }
        } catch (IOException e) {
            result.put("success", false);
            result.put("message", "文件读取失败");
        }

        return result;
    }

    /**
     * 获取可下载文件列表
     * GET /api/challenge/file-op/traversal/list
     */
    @GetMapping("/list")
    public Map<String, Object> listFiles() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("files", AVAILABLE_FILES);
        result.put("basePath", "/downloads/");
        return result;
    }
}
