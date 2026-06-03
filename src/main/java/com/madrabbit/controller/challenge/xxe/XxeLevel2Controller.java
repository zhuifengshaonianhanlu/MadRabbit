package com.madrabbit.controller.challenge.xxe;

import com.madrabbit.service.FlagService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.w3c.dom.Document;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

@RestController
@RequestMapping("/api/challenge/xxe/level2")
public class XxeLevel2Controller {

    @Autowired
    private FlagService flagService;

    // flag 文件内容就是真实 flag，外带出去直接可见
    private static final String FLAG_VALUE = "flag{XX3_00B_Ext4ct10n}";

    /**
     * 校验 XML 格式 — 盲 XXE，不回显解析内容
     */
    @PostMapping(value = "/validate", consumes = {"application/xml", "text/xml"})
    public Map<String, Object> validateXml(@RequestBody String xmlContent) {
        Map<String, Object> result = new LinkedHashMap<>();

        try {
            Map<String, Object> status = flagService.getStatus("xxe", "level2");
            if (status != null && "未开始".equals(status.get("status"))) {
                flagService.updateStatus("xxe", "level2", "进行中");
            }
        } catch (Exception e) { /* ignore */ }

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            builder.setEntityResolver(new OobEntityResolver());
            Document doc = builder.parse(new ByteArrayInputStream(xmlContent.getBytes(StandardCharsets.UTF_8)));

            result.put("success", true);
            result.put("valid", true);
            result.put("message", "XML format is valid.");
        } catch (Exception e) {
            result.put("success", true);
            result.put("valid", false);
            result.put("message", "XML format error: " + e.getMessage());
        }

        return result;
    }

    private static class OobEntityResolver implements EntityResolver {
        @Override
        public InputSource resolveEntity(String publicId, String systemId) {
            // flag 虚拟文件：直接返回 flag 内容
            if (systemId != null && systemId.contains("/flag/xxe-level2")) {
                return new InputSource(new StringReader(FLAG_VALUE));
            }
            // 其他请求返回 null，走默认解析（真实读取文件 / 真实发起 HTTP 请求）
            return null;
        }
    }
}
