package com.madrabbit.controller.challenge.xxe;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.madrabbit.service.FlagService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

@RestController
@RequestMapping("/api/challenge/xxe/level3")
public class XxeLevel3Controller {

    @Autowired
    private FlagService flagService;

    private static final String FLAG_FILE_CONTENT = "XXE_LEVEL3_CONTENT_TYPE_2024";
    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 更新用户资料 — 同时支持 JSON 和 XML
     * 前端只用 JSON，但后端也接受 XML（存在 XXE 漏洞）
     */
    @PostMapping(value = "/profile/update", consumes = {"application/json", "application/xml", "text/xml"})
    public Map<String, Object> updateProfile(
            @RequestHeader("Content-Type") String contentType,
            @RequestBody String body) {

        Map<String, Object> result = new LinkedHashMap<>();

        // 更新关卡状态
        try {
            Map<String, Object> status = flagService.getStatus("xxe", "level3");
            if (status != null && "未开始".equals(status.get("status"))) {
                flagService.updateStatus("xxe", "level3", "进行中");
            }
        } catch (Exception e) { /* ignore */ }

        try {
            Map<String, String> profile;

            if (contentType != null && (contentType.contains("xml"))) {
                // XML 分支 — 存在 XXE 漏洞
                profile = parseXmlProfile(body);
            } else {
                // JSON 分支 — 正常处理
                profile = parseJsonProfile(body);
            }

            result.put("success", true);
            result.put("message", "Profile updated successfully.");
            result.put("profile", profile);

            // 检查是否包含 flag 文件内容
            for (String value : profile.values()) {
                if (value != null && value.contains(FLAG_FILE_CONTENT)) {
                    String flag = flagService.getFlag("xxe", "level3");
                    result.put("flag", flag);
                    break;
                }
            }

        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "Error processing request: " + e.getMessage());
        }

        return result;
    }

    /**
     * 获取当前用户资料（供前端展示）
     */
    @GetMapping("/profile")
    public Map<String, Object> getProfile() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        Map<String, String> profile = new LinkedHashMap<>();
        profile.put("username", "alice");
        profile.put("email", "alice@example.com");
        profile.put("bio", "Security enthusiast");
        result.put("profile", profile);
        return result;
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> parseJsonProfile(String body) throws Exception {
        Map<String, Object> raw = objectMapper.readValue(body, Map.class);
        Map<String, String> profile = new LinkedHashMap<>();
        profile.put("username", String.valueOf(raw.getOrDefault("username", "")));
        profile.put("email", String.valueOf(raw.getOrDefault("email", "")));
        profile.put("bio", String.valueOf(raw.getOrDefault("bio", "")));
        return profile;
    }

    private Map<String, String> parseXmlProfile(String body) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        // 【漏洞】不禁用外部实体
        DocumentBuilder builder = factory.newDocumentBuilder();
        builder.setEntityResolver(new ProfileEntityResolver());

        Document doc = builder.parse(new ByteArrayInputStream(body.getBytes(StandardCharsets.UTF_8)));
        doc.getDocumentElement().normalize();

        Map<String, String> profile = new LinkedHashMap<>();
        Element root = doc.getDocumentElement();
        NodeList children = root.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            if (children.item(i) instanceof Element) {
                Element el = (Element) children.item(i);
                profile.put(el.getTagName(), el.getTextContent());
            }
        }
        return profile;
    }

    private static class ProfileEntityResolver implements EntityResolver {
        @Override
        public InputSource resolveEntity(String publicId, String systemId) {
            if (systemId != null && systemId.contains("/flag/xxe-level3")) {
                return new InputSource(new StringReader(FLAG_FILE_CONTENT));
            }
            if (systemId != null && systemId.startsWith("file://") && !systemId.contains("/flag/")) {
                return new InputSource(new StringReader("[Access Denied]"));
            }
            return null;
        }
    }
}
