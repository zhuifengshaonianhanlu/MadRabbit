package com.madrabbit.controller.challenge.ssrf;

import com.madrabbit.service.FlagService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * SSRF过滤绕过关卡 - 有白名单过滤的URL请求
 *
 * 业务场景：页面提供URL请求功能，但服务端有简单的黑名单过滤
 *
 * 漏洞点：黑名单只检测常见的明文内网地址（127.0.0.1、localhost等），
 *         未覆盖IP混淆表示（0x7f000001、2130706433、[::1]等），
 *         服务端真实发起HTTP请求，绕过过滤后可访问内网服务获取flag
 */
@RestController
@RequestMapping("/api/challenge/ssrf/bypass")
public class SsrfBypassController {

    @Autowired
    private FlagService flagService;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * 内部隐藏API - 模拟内网中的敏感服务
     * 通过绕过黑名单过滤后的SSRF可以访问到此接口
     */
    @GetMapping("/secret-info")
    public ResponseEntity<Map<String, Object>> secretInfo() {
        Map<String, Object> data = new HashMap<>();
        String flag = flagService.getFlag("ssrf", "level3");
        data.put("flag", flag);
        data.put("info", "SSRF-BYPASS-SECRET-ACCESS");
        data.put("message", "You have accessed the internal secret service via SSRF filter bypass!");
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(data);
    }

    /**
     * URL请求接口 - 真实发起HTTP请求
     * POST /api/challenge/ssrf/bypass/request
     * Body: { "url": "用户输入的URL" }
     *
     * 逻辑流程：
     * 1. 黑名单过滤：检测常见明文内网地址，命中则拦截
     * 2. 真实请求：用 RestTemplate 对用户提供的URL发起真实HTTP请求
     * 3. Flag判定：如果真实响应中包含flag，说明绕过成功并成功访问到内网服务
     */
    @PostMapping("/request")
    public Map<String, Object> request(@RequestBody Map<String, String> request) {
        Map<String, Object> result = new HashMap<>();
        String url = request.get("url");

        if (url == null || url.trim().isEmpty()) {
            result.put("success", false);
            result.put("message", "请输入要请求的URL");
            return result;
        }

        // 更新关卡状态为"进行中"
        try {
            Map<String, Object> status = flagService.getStatus("ssrf", "level3");
            if (status != null && "未开始".equals(status.get("status"))) {
                flagService.updateStatus("ssrf", "level3", "进行中");
            }
        } catch (Exception e) {
            // 忽略状态更新异常
        }

        // 黑名单过滤（简单的字符串匹配，存在绕过漏洞）
        boolean passedBasicFilter = !containsBasicInternalAddress(url);

        if (!passedBasicFilter) {
            // 被黑名单拦截
            result.put("success", false);
            result.put("blocked", true);
            result.put("message", "❌ 请求被拦截！检测到禁止访问的内网地址。提示：尝试使用IP编码绕过技术。");
            return result;
        }

        // 通过黑名单后，真实发起HTTP请求
        String bypassTechnique = detectBypassTechnique(url);

        try {
            ResponseEntity<byte[]> response = restTemplate.exchange(
                    url, HttpMethod.GET, new HttpEntity<>(createHeaders()), byte[].class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                String content = new String(response.getBody(), StandardCharsets.UTF_8);
                result.put("success", true);
                result.put("message", "请求成功: " + url);
                result.put("content", content);

                // 检测响应中是否包含flag，判断是否成功通过SSRF访问到内网服务
                String flag = flagService.getFlag("ssrf", "level3");
                if (flag != null && content.contains(flag)) {
                    result.put("flag", flag);
                    result.put("ssrf_detected", true);
                    if (bypassTechnique != null) {
                        result.put("bypass_technique", bypassTechnique);
                    }
                    result.put("message", "⚠️ SSRF Bypass Detected! 过滤绕过成功"
                            + (bypassTechnique != null ? "，绕过技术: " + bypassTechnique : "")
                            + "！Flag: " + flag);
                }
            } else {
                result.put("success", false);
                result.put("message", "请求失败，状态码: " + response.getStatusCode());
            }

        } catch (Exception e) {
            // 真实请求失败
            if (bypassTechnique != null) {
                // 检测到绕过技术但请求未能到达目标（该绕过方式在当前环境中可能不被解析）
                result.put("success", false);
                result.put("bypass_technique", bypassTechnique);
                result.put("message", "⚠️ 检测到绕过技术: " + bypassTechnique
                        + "，但请求未能成功到达目标。该绕过方式在当前Java环境中可能不被解析，请尝试其他绕过技术。");
            } else {
                result.put("success", false);
                result.put("message", "请求失败: " + e.getMessage());
            }
        }

        return result;
    }

    /**
     * 检测基本的内网地址（简单黑名单过滤，刻意设计为不完整）
     */
    private boolean containsBasicInternalAddress(String url) {
        if (url == null) return false;
        String lower = url.toLowerCase();

        // 基本过滤：只检测常见明文格式，IP混淆变体均可绕过
        return lower.contains("127.0.0.1") ||
               lower.contains("localhost") ||
               lower.contains("192.168.") ||
               lower.contains("10.0.") ||
               lower.contains("172.16.");
    }

    /**
     * 检测是否使用了绕过技术（仅用于教学信息反馈，不影响flag判定）
     * @return 检测到的绕过技术名称，如果没有检测到返回null
     */
    private String detectBypassTechnique(String url) {
        if (url == null) return null;
        String lower = url.toLowerCase();

        // 十六进制IP表示: 0x7f000001
        if (lower.matches(".*0x[0-9a-f]+.*") || lower.contains("0x7f")) {
            return "Hex IP (十六进制IP)";
        }

        // 十进制整数IP表示: 2130706433 (127.0.0.1的十进制)
        if (lower.matches(".*\\b2130706433\\b.*")) {
            return "Decimal IP (十进制IP)";
        }

        // 八进制IP表示: 0177.0.0.1
        if (lower.matches(".*0[0-7]{3}\\.[0-7]+\\.[0-7]+\\.[0-7]+.*") ||
            lower.matches(".*\\b0177\\b.*")) {
            return "Octal IP (八进制IP)";
        }

        // IPv6表示
        if (lower.contains("[::1]") || lower.contains("[::]") ||
            lower.matches(".*\\[::ffff:127\\.0\\.0\\.1\\].*") ||
            lower.contains("::ffff:")) {
            return "IPv6 Format";
        }

        // URL编码
        if (lower.contains("%31%32%37") || lower.contains("%6c%6f%63%61%6c")) {
            return "URL Encoding";
        }

        // DNS重绑定
        if (lower.contains("xip.io") || lower.contains("nip.io") ||
            lower.contains("sslip.io") || lower.contains(".localtest.me")) {
            return "DNS Rebinding";
        }

        // 短URL或特殊域名
        if (lower.contains("127.0.0.1.xip.io") || lower.contains("spoofed.burpcollaborator.net")) {
            return "Domain Spoofing";
        }

        // 混合表示: 127.1, 127.0.1
        if (lower.matches(".*\\b127\\.1\\b.*") || lower.matches(".*\\b127\\.0\\.1\\b.*")) {
            return "Short IP Format";
        }

        // 特殊的localhost变体
        if (lower.contains("localtest.me") || lower.contains("lvh.me") ||
            lower.contains("vcap.me")) {
            return "Localhost Domain Alias";
        }

        // Unicode编码绕过
        if (url.contains("①") || url.contains("②") || url.contains("⑦")) {
            return "Unicode Bypass";
        }

        return null;
    }

    /**
     * 创建HTTP请求头
     */
    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Collections.singletonList(MediaType.ALL));
        return headers;
    }
}
