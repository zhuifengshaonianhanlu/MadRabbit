package com.madrabbit.controller.challenge.infoleak;

import com.madrabbit.service.FlagService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.*;

/**
 * 敏感信息泄露关卡 - Level 3: .git 信息泄露 (Git Repository Exposure)
 * 场景：逐步探测 .git 目录，从提交历史中还原敏感配置信息
 */
@RestController
@RequestMapping("/api/challenge/info-leak/git")
public class GitLeakController {

    @Autowired
    private FlagService flagService;

    /**
     * 返回企业官网页面数据
     * GET /api/challenge/info-leak/git/page
     */
    @GetMapping("/page")
    public Map<String, Object> getPage() {
        Map<String, Object> result = new HashMap<>();

        // 更新关卡状态为"进行中"
        try {
            Map<String, Object> status = flagService.getStatus("info-leak", "level3");
            if (status != null && "未开始".equals(status.get("status"))) {
                flagService.updateStatus("info-leak", "level3", "进行中");
            }
        } catch (Exception e) {
            // 忽略状态更新异常
        }

        result.put("success", true);
        result.put("company", "TechNova Solutions");
        result.put("slogan", "Empowering Digital Transformation");
        result.put("services", Arrays.asList("Cloud Computing", "AI & ML", "Cybersecurity", "Data Analytics"));
        return result;
    }

    /**
     * 模拟路径探测
     * GET /api/challenge/info-leak/git/probe?path={path}
     */
    @GetMapping("/probe")
    public Map<String, Object> probe(@RequestParam(value = "path", defaultValue = "") String path) {
        Map<String, Object> result = new LinkedHashMap<>();

        if (path == null || path.trim().isEmpty()) {
            result.put("success", false);
            result.put("status", 400);
            result.put("message", "Parameter 'path' is required");
            return result;
        }

        // 标准化路径
        String normalizedPath = path.trim();
        if (!normalizedPath.startsWith("/")) {
            normalizedPath = "/" + normalizedPath;
        }

        // 路由匹配
        if (normalizedPath.equals("/.git/config")) {
            return buildGitConfig();
        } else if (normalizedPath.equals("/.git/HEAD")) {
            return buildGitHead();
        } else if (normalizedPath.equals("/.git/logs/HEAD")) {
            return buildGitLogs();
        } else if (normalizedPath.equals("/.git/objects/abc123") || normalizedPath.equals("/.git/objects/abc123f")) {
            return buildGitObject();
        } else if (normalizedPath.startsWith("/.git/")) {
            // 其他 .git 子路径 - 返回403
            result.put("success", false);
            result.put("status", 403);
            result.put("message", "Forbidden: Access to " + normalizedPath + " is restricted");
            result.put("hint", "Try exploring other .git paths like /config, /HEAD, /logs/HEAD, or /objects/{hash}");
            result.put("hint_zh", "尝试探索其他 .git 路径，如 /config, /HEAD, /logs/HEAD, 或 /objects/{hash}");
            return result;
        } else {
            // 非 .git 路径 - 返回404
            result.put("success", false);
            result.put("status", 404);
            result.put("message", "Not Found: " + normalizedPath);
            return result;
        }
    }

    /**
     * /.git/config - 返回 git 配置信息
     */
    private Map<String, Object> buildGitConfig() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("status", 200);
        result.put("path", "/.git/config");
        result.put("content_type", "text/plain");

        String content = "[core]\n" +
                "    repositoryformatversion = 0\n" +
                "    filemode = true\n" +
                "    bare = false\n" +
                "    logallrefupdates = true\n" +
                "[remote \"origin\"]\n" +
                "    url = git@gitlab.technova-internal.com:web-team/corporate-site.git\n" +
                "    fetch = +refs/heads/*:refs/remotes/origin/*\n" +
                "[branch \"main\"]\n" +
                "    remote = origin\n" +
                "    merge = refs/heads/main\n" +
                "[user]\n" +
                "    name = deploy-bot\n" +
                "    email = deploy@technova-internal.com\n";

        result.put("content", content);
        result.put("hint", "Git repository exposed! Try accessing /.git/HEAD and /.git/logs/HEAD for more info.");
        result.put("hint_zh", "Git 仓库暴露！尝试访问 /.git/HEAD 和 /.git/logs/HEAD 获取更多信息。");
        return result;
    }

    /**
     * /.git/HEAD - 返回当前分支引用
     */
    private Map<String, Object> buildGitHead() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("status", 200);
        result.put("path", "/.git/HEAD");
        result.put("content_type", "text/plain");
        result.put("content", "ref: refs/heads/main\n");
        result.put("hint", "Current branch is 'main'. Check /.git/logs/HEAD for commit history.");
        result.put("hint_zh", "当前分支为 'main'。查看 /.git/logs/HEAD 获取提交历史。");
        return result;
    }

    /**
     * /.git/logs/HEAD - 返回提交日志
     */
    private Map<String, Object> buildGitLogs() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("status", 200);
        result.put("path", "/.git/logs/HEAD");
        result.put("content_type", "text/plain");

        String content = "0000000 e7f2a91 deploy-bot <deploy@technova-internal.com> 1703001600 +0800\tcommit (initial): init corporate site\n" +
                "e7f2a91 abc123f deploy-bot <deploy@technova-internal.com> 1703088000 +0800\tcommit: add database config with credentials\n" +
                "abc123f 5d9e8b2 deploy-bot <deploy@technova-internal.com> 1703174400 +0800\tcommit: remove database password from config - security fix\n" +
                "5d9e8b2 f4a6c31 deploy-bot <deploy@technova-internal.com> 1703260800 +0800\tcommit: update homepage content\n" +
                "f4a6c31 8b2e1d7 deploy-bot <deploy@technova-internal.com> 1703347200 +0800\tcommit: add contact page\n" +
                "8b2e1d7 2c7f9a4 deploy-bot <deploy@technova-internal.com> 1703433600 +0800\tcommit: deploy production v1.2.0\n";

        result.put("content", content);
        result.put("hint", "Notice commit abc123f: 'add database config with credentials' - this was removed in the next commit. Try /.git/objects/abc123 to view that commit's content.");
        result.put("hint_zh", "注意提交 abc123f: '添加数据库配置和凭据' — 在下一次提交中被删除了。尝试访问 /.git/objects/abc123 查看该提交的内容。");
        return result;
    }

    /**
     * /.git/objects/abc123 - 返回包含 flag 的旧配置内容
     */
    private Map<String, Object> buildGitObject() {
        String flag = flagService.getFlag("info-leak", "level3");

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("status", 200);
        result.put("path", "/.git/objects/abc123f");
        result.put("content_type", "text/plain");
        result.put("object_type", "commit");
        result.put("commit_message", "add database config with credentials");
        result.put("author", "deploy-bot <deploy@technova-internal.com>");
        result.put("date", "2023-12-20T16:00:00+08:00");

        String fileContent = "# config/database.yml\n" +
                "# Database Configuration - TechNova Corporate Site\n" +
                "# WARNING: Do not commit credentials to version control!\n\n" +
                "production:\n" +
                "  adapter: mysql2\n" +
                "  host: db-prod.technova-internal.com\n" +
                "  port: 3306\n" +
                "  database: technova_corp\n" +
                "  username: corp_admin\n" +
                "  password: Pr0d_DB@2024!Secure\n" +
                "  pool: 25\n" +
                "  timeout: 5000\n\n" +
                "# Admin Panel Credentials\n" +
                "admin_panel:\n" +
                "  secret_key: sk-technova-9f8e7d6c5b4a3210\n" +
                "  jwt_secret: " + flag + "\n" +
                "  api_token: tn-api-live-xyz789abc456\n";

        result.put("file_content", fileContent);
        result.put("warning", "Sensitive credentials found in git history! Even after removal, data persists in git objects.");
        result.put("warning_zh", "在 git 历史中发现敏感凭据！即使删除，数据仍保留在 git 对象中。");
        return result;
    }
}
