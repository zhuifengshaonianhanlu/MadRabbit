package com.madrabbit.controller.challenge.ssrf;

import com.madrabbit.service.FlagService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * SSRF重定向绕过关卡 - 域名安全检测服务
 *
 * 业务场景：用户输入域名，服务端先做DNS解析检查IP是否为内网地址，通过后再发起HTTP请求。
 *
 * 漏洞点：服务端对用户输入的域名进行了DNS解析，并正确拒绝了解析到内网IP的请求，
 *         但未关闭HTTP客户端的自动重定向跟随功能。攻击者可以搭建一个恶意服务器，
 *         域名解析到外网IP通过检查，但该服务器返回302重定向到内网地址，
 *         HTTP客户端自动跟随重定向从而访问内网服务。
 */
@RestController
@RequestMapping("/api/challenge/ssrf/redirect")
public class SsrfRedirectController {

    @Autowired
    private FlagService flagService;

    /**
     * 内部隐藏API - 模拟内网中的敏感服务
     * 通过重定向绕过DNS检查后的SSRF可以访问到此接口
     */
    @GetMapping("/secret-info")
    public ResponseEntity<Map<String, Object>> secretInfo() {
        Map<String, Object> data = new HashMap<>();
        String flag = flagService.getFlag("ssrf", "level4");
        data.put("flag", flag);
        data.put("info", "SSRF-REDIRECT-BYPASS-SECRET");
        data.put("message", "You have accessed the internal secret service via SSRF redirect bypass!");
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(data);
    }

    /**
     * 域名检测接口 - 漏洞点
     * POST /api/challenge/ssrf/redirect/detect
     * Body: { "domain": "example.com" }
     *
     * 逻辑流程：
     * 1. 域名格式校验（仅允许合法域名，不允许IP/协议头/端口/路径）
     * 2. DNS解析获取IP
     * 3. 检查IP是否为内网地址
     * 4. 若为内网IP → 返回拦截信息
     * 5. 若为外网IP → 发起 HTTP GET 请求（未关闭自动重定向 - 漏洞）
     * 6. 返回请求结果
     */
    @PostMapping("/detect")
    public Map<String, Object> detect(@RequestBody Map<String, String> request) {
        Map<String, Object> result = new HashMap<>();
        String url = request.get("domain");

        if (url == null || url.trim().isEmpty()) {
            result.put("success", false);
            result.put("message", "Please enter a URL");
            return result;
        }

        url = url.trim();

        // 1. URL格式校验：必须以 http:// 或 https:// 开头，域名部分合法，不允许IP
        if (!isValidUrl(url)) {
            result.put("success", false);
            result.put("message", "Invalid URL format. Must start with http:// or https://, domain names only (no IP addresses).");
            return result;
        }

        // 从URL中提取域名
        String domain = extractDomain(url);

        // 更新关卡状态为"进行中"
        try {
            Map<String, Object> status = flagService.getStatus("ssrf", "level4");
            if (status != null && "未开始".equals(status.get("status"))) {
                flagService.updateStatus("ssrf", "level4", "进行中");
            }
        } catch (Exception e) {
            // 忽略状态更新异常
        }

        try {
            // 2. DNS解析
            InetAddress address = InetAddress.getByName(domain);
            String resolvedIp = address.getHostAddress();
            String requestUrl = url;

            result.put("domain", domain);
            result.put("resolved_ip", resolvedIp);

            // 3. 内网IP检查
            if (isInternalIp(resolvedIp)) {
                result.put("success", false);
                result.put("blocked", true);
                result.put("ip_check", "blocked");
                result.put("message", "❌ Security check failed! Domain resolved to internal address " + resolvedIp + ", request blocked.");
                return result;
            }

            result.put("ip_check", "pass");

            // 4. 发起HTTP请求（漏洞点：未关闭自动重定向）
            // ❌ 以下是存在漏洞的代码：RestTemplate 默认跟随重定向
            RestTemplate restTemplate = new RestTemplate();

            // ✅ 安全修复（代码中注释掉，用于教学演示）：
            // SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory() {
            //     @Override
            //     protected void prepareConnection(HttpURLConnection connection, String httpMethod) throws IOException {
            //         super.prepareConnection(connection, httpMethod);
            //         connection.setInstanceFollowRedirects(false);  // 关闭重定向跟随
            //     }
            // };
            // RestTemplate restTemplate = new RestTemplate(factory);

            HttpHeaders headers = new HttpHeaders();
            headers.setAccept(Collections.singletonList(MediaType.ALL));
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<byte[]> response = restTemplate.exchange(
                    requestUrl, HttpMethod.GET, entity, byte[].class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                String content = new String(response.getBody(), StandardCharsets.UTF_8);
                result.put("success", true);
                result.put("content", content);

                // 5. 检查响应内容是否包含flag（重定向绕过成功）
                String flag = flagService.getFlag("ssrf", "level4");
                if (flag != null && content.contains(flag)) {
                    result.put("flag", flag);
                    result.put("ssrf_detected", true);
                    result.put("redirect_detected", true);
                    result.put("final_url", "http://127.0.0.1:8080/api/challenge/ssrf/redirect/secret-info");
                    result.put("message", "⚠️ SSRF Redirect Bypass Detected! DNS check bypassed via redirect! Flag: " + flag);
                } else {
                    result.put("message", "Domain resolved safely, request successful.");
                }
            } else {
                result.put("success", false);
                result.put("message", "Request failed, status code: " + response.getStatusCode());
            }

        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "Request failed: " + e.getMessage());
        }

        return result;
    }

    /**
     * URL格式校验：必须以 http:// 或 https:// 开头，域名部分合法，不允许IP地址
     */
    private boolean isValidUrl(String url) {
        if (url == null || url.isEmpty()) return false;
        String urlRegex = "^https?://([a-zA-Z0-9]([a-zA-Z0-9\\-]{0,61}[a-zA-Z0-9])?\\.)+[a-zA-Z]{2,}(/.*)?$";
        return url.matches(urlRegex);
    }

    /**
     * 从URL中提取域名部分
     */
    private String extractDomain(String url) {
        // 去掉协议头
        String noProtocol = url.replaceFirst("^https?://", "");
        // 去掉路径部分
        int slashIndex = noProtocol.indexOf('/');
        if (slashIndex != -1) {
            noProtocol = noProtocol.substring(0, slashIndex);
        }
        return noProtocol.toLowerCase();
    }

    /**
     * 检查IP是否为内网地址
     * 包括：127.x.x.x, 10.x.x.x, 172.16-31.x.x, 192.168.x.x, 0.0.0.0, ::1
     */
    private boolean isInternalIp(String ip) {
        if (ip == null) return false;
        return ip.startsWith("127.") ||
               ip.startsWith("10.") ||
               ip.startsWith("192.168.") ||
               ip.equals("0.0.0.0") ||
               ip.equals("::1") ||
               isIn172Range(ip);
    }

    /**
     * 检查是否在172.16.0.0 - 172.31.255.255范围内
     */
    private boolean isIn172Range(String ip) {
        if (!ip.startsWith("172.")) return false;
        try {
            String[] parts = ip.split("\\.");
            if (parts.length != 4) return false;
            int second = Integer.parseInt(parts[1]);
            return second >= 16 && second <= 31;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
