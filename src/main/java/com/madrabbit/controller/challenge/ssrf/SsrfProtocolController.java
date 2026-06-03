package com.madrabbit.controller.challenge.ssrf;

import com.madrabbit.service.FlagService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * SSRF协议利用关卡 - 文件导入功能
 *
 * 业务场景：页面提供"文件导入"功能，用户输入URL后服务端获取内容。
 *
 * 漏洞点：服务端使用 java.net.URL 处理用户输入，该类原生支持多种协议：
 *   - http:// / https:// → 发起HTTP请求（可访问内网服务）
 *   - file:// → 读取服务器本地文件
 * 服务端未对协议类型和目标地址做任何校验。
 *
 * 攻击方式：
 *   1. file:// 协议：读取 classpath 下的 internal-config.properties 获取 flag
 *   2. http:// 协议：访问 http://127.0.0.1:8080/api/challenge/ssrf/protocol/secret-info 获取 flag
 */
@RestController
@RequestMapping("/api/challenge/ssrf/protocol")
public class SsrfProtocolController {

    @Autowired
    private FlagService flagService;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * 内部隐藏API - 模拟内网中的敏感服务
     * 通过 HTTP 协议利用 SSRF 可以访问到此接口
     */
    @GetMapping("/secret-info")
    public ResponseEntity<Map<String, Object>> secretInfo() {
        Map<String, Object> data = new HashMap<>();
        String flag = flagService.getFlag("ssrf", "level2");
        data.put("flag", flag);
        data.put("info", "SSRF-PROTOCOL-SECRET-ACCESS");
        data.put("message", "You have accessed the internal secret service via SSRF protocol exploitation!");
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(data);
    }

    /**
     * 文件导入接口 - 使用 java.net.URL 真实获取资源内容
     * POST /api/challenge/ssrf/protocol/import
     * Body: { "url": "用户输入的URL" }
     *
     * 漏洞核心：java.net.URL 原生支持 file:// 和 http:// 等多种协议
     * 当用户传入 file:///path/to/file 时，URL.openStream() 会真实读取本地文件
     * 当用户传入 http://127.0.0.1:port/... 时，会真实访问内网服务
     */
    @PostMapping("/import")
    public Map<String, Object> importFile(@RequestBody Map<String, String> request) {
        Map<String, Object> result = new HashMap<>();
        String url = request.get("url");

        if (url == null || url.trim().isEmpty()) {
            result.put("success", false);
            result.put("message", "请输入要导入的文件URL");
            return result;
        }

        // 更新关卡状态为"进行中"
        try {
            Map<String, Object> status = flagService.getStatus("ssrf", "level2");
            if (status != null && "未开始".equals(status.get("status"))) {
                flagService.updateStatus("ssrf", "level2", "进行中");
            }
        } catch (Exception e) {
            // 忽略状态更新异常
        }

        String lowerUrl = url.toLowerCase().trim();

        try {
            // ========== 使用 java.net.URL 真实获取资源 ==========
            // 这是漏洞核心：URL 类原生支持 file:// 和 http:// 协议
            // 没有任何协议白名单校验，用户可以指定任意支持的协议
            java.net.URL targetUrl = new URL(url);

            String protocol = targetUrl.getProtocol().toLowerCase();

            if (protocol.equals("file")) {
                // file:// 协议 - 真实读取服务器本地文件
                String fileContent = readFileContent(targetUrl);
                result.put("success", true);
                result.put("protocol", "file://");
                result.put("message", "File loaded via file:// protocol: " + url);
                result.put("content", fileContent);

                // file:// 协议读取成功即判定为SSRF攻击——任意文件读取本身就是漏洞
                if (fileContent != null && !fileContent.trim().isEmpty()) {
                    String flag = flagService.getFlag("ssrf", "level2");
                    if (flag != null) {
                        result.put("flag", flag);
                        result.put("ssrf_detected", true);
                        result.put("message", "⚠️ SSRF Protocol Attack Detected! 通过 file:// 协议读取到服务器本地文件！Flag: " + flag);
                    }
                }

            } else if (protocol.equals("http") || protocol.equals("https")) {
                // http/https 协议 - 真实发起HTTP请求
                // 可用于访问内网服务（如 127.0.0.1 上的 secret-info 接口）
                ResponseEntity<byte[]> response = restTemplate.exchange(
                        url, HttpMethod.GET, new HttpEntity<>(createHeaders()), byte[].class);

                if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                    String contentType = response.getHeaders().getContentType() != null
                            ? response.getHeaders().getContentType().toString() : "";

                    if (contentType.contains("application/json")) {
                        // 访问到了内网API（返回JSON），检测是否包含flag
                        String jsonStr = new String(response.getBody(), StandardCharsets.UTF_8);
                        result.put("success", true);
                        result.put("protocol", "http://");
                        result.put("message", "HTTP request to: " + url);
                        result.put("content", jsonStr);

                        // 检测JSON中是否包含flag
                        String flag = flagService.getFlag("ssrf", "level2");
                        if (flag != null && jsonStr.contains(flag)) {
                            result.put("flag", flag);
                            result.put("ssrf_detected", true);
                            result.put("message", "⚠️ SSRF Protocol Attack Detected! 通过 http:// 协议访问到内网API！Flag: " + flag);
                        }
                    } else {
                        // 普通HTTP响应
                        String content = new String(response.getBody(), StandardCharsets.UTF_8);
                        result.put("success", true);
                        result.put("protocol", "http://");
                        result.put("message", "HTTP request to: " + url);
                        result.put("content", truncate(content, 5000));
                    }
                } else {
                    result.put("success", false);
                    result.put("message", "HTTP request failed: " + response.getStatusCode());
                }

            } else {
                // 不支持的协议
                result.put("success", false);
                result.put("message", "不支持的协议: " + protocol + "。当前仅支持 http://, https://, file://");
            }

        } catch (java.net.MalformedURLException e) {
            result.put("success", false);
            result.put("message", "URL格式错误: " + e.getMessage());
        } catch (IOException e) {
            result.put("success", false);
            result.put("message", "读取资源失败: " + e.getMessage());
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "请求失败: " + e.getMessage());
        }

        return result;
    }

    /**
     * 使用 java.net.URL 读取 file:// 协议的文件内容
     * 这是真实的文件读取，不是模拟
     */
    private String readFileContent(URL fileUrl) throws IOException {
        StringBuilder content = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(fileUrl.openStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
        }
        return content.toString();
    }

    /**
     * 创建HTTP请求头
     */
    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Collections.singletonList(MediaType.ALL));
        return headers;
    }

    /**
     * 截断过长的内容
     */
    private String truncate(String content, int maxLength) {
        if (content == null) return "";
        if (content.length() <= maxLength) return content;
        return content.substring(0, maxLength) + "\n... (content truncated)";
    }
}
