package com.madrabbit.controller.challenge.ssrf;

import com.madrabbit.service.FlagService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * SSRF基础关卡 - NBA 1996黄金一代介绍页
 *
 * 业务场景：页面展示NBA 1996选秀球员信息，点击"下载头像"时，
 * 服务端根据传入的imageUrl发起真实的HTTP请求下载图片资源。
 *
 * SSRF漏洞点：imageUrl参数由前端传入，用户可以篡改该参数指向内网服务。
 * 内网存在一个隐藏的API服务 /api/internal/ssrf-secret ，访问即返回flag。
 */
@RestController
@RequestMapping("/api/challenge/ssrf/basic")
public class SsrfBasicController {

    @Autowired
    private FlagService flagService;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * 内部隐藏API - 模拟内网中的敏感服务
     * 正常情况下前端无法直接访问（假设只在内网开放），
     * 但通过SSRF可以让服务端代理访问此接口，从而获取flag。
     */
    @GetMapping("/secret-info")
    public ResponseEntity<Map<String, Object>> secretInfo() {
        Map<String, Object> data = new HashMap<>();
        String flag = flagService.getFlag("ssrf", "level1");
        data.put("flag", flag);
        data.put("info", "SSRF-SECRET-ACCESS-GRANTED");
        data.put("message", "You have accessed the internal secret service via SSRF!");
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(data);
    }

    /**
     * 图片下载接口 - 服务端根据imageUrl发起真实HTTP请求获取资源
     * POST /api/challenge/ssrf/basic/download
     * Body: { "imageUrl": "http://host:port/path/to/image.jpg" }
     *
     * 正常使用时，imageUrl指向静态资源服务器上的球员头像。
     * 攻击者可篡改imageUrl指向内网服务（如本接口的secret-info端点），触发SSRF。
     */
    @PostMapping("/download")
    public ResponseEntity<byte[]> downloadImage(@RequestBody Map<String, String> request) {
        String imageUrl = request.get("imageUrl");

        if (imageUrl == null || imageUrl.trim().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body("{\"success\":false,\"message\":\"imageUrl is required\"}".getBytes(StandardCharsets.UTF_8));
        }

        try {
            // 服务端发起真实的HTTP请求
            HttpHeaders headers = new HttpHeaders();
            headers.setAccept(Collections.singletonList(MediaType.ALL));
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<byte[]> response = restTemplate.exchange(
                    imageUrl,
                    HttpMethod.GET,
                    entity,
                    byte[].class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                byte[] body = response.getBody();

                // 检测是否访问到了内网的secret-info接口（返回的是JSON且包含flag）
                String contentType = response.getHeaders().getContentType() != null
                        ? response.getHeaders().getContentType().toString() : "";

                if (contentType.contains("application/json")) {
                    // SSRF成功 - 访问到了内网API，将JSON响应原样返回
                    // 前端检测到JSON中包含flag字段即可提示用户
                    return ResponseEntity.ok()
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(body);
                }

                // 正常图片下载，返回图片二进制数据
                return ResponseEntity.ok()
                        .contentType(response.getHeaders().getContentType())
                        .header("Content-Disposition", "attachment; filename=\"downloaded_image.jpg\"")
                        .body(body);
            } else {
                return ResponseEntity.status(response.getStatusCode())
                        .body("{\"success\":false,\"message\":\"Failed to fetch resource\"}".getBytes(StandardCharsets.UTF_8));
            }

        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(("{\"success\":false,\"message\":\"Request failed: " + e.getMessage() + "\"}").getBytes(StandardCharsets.UTF_8));
        }
    }
}
