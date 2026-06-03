package com.madrabbit.controller.challenge.xxe;

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
@RequestMapping("/api/challenge/xxe/level1")
public class XxeLevel1Controller {

    @Autowired
    private FlagService flagService;

    // 虚拟 flag 文件内容（不使用真实文件系统）
    private static final String FLAG_FILE_CONTENT = "XXE_LEVEL1_SECRET_TOKEN_2024";

    /**
     * 解析 XML 配置 — 存在 XXE 漏洞
     * POST /api/challenge/xxe/level1/parse
     * Content-Type: application/xml
     * Body: XML 字符串
     */
    @PostMapping(value = "/parse", consumes = {"application/xml", "text/xml"})
    public Map<String, Object> parseXml(@RequestBody String xmlContent) {
        Map<String, Object> result = new LinkedHashMap<>();

        // 更新关卡状态
        try {
            Map<String, Object> status = flagService.getStatus("xxe", "level1");
            if (status != null && "未开始".equals(status.get("status"))) {
                flagService.updateStatus("xxe", "level1", "进行中");
            }
        } catch (Exception e) {
            // 忽略
        }

        try {
            // 【漏洞核心】不禁用外部实体解析
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            // 故意不设置安全特性，允许 XXE
            DocumentBuilder builder = factory.newDocumentBuilder();

            // 自定义 EntityResolver：拦截 file:///flag/ 请求，返回虚拟内容
            builder.setEntityResolver(new FlagEntityResolver());

            Document doc = builder.parse(new ByteArrayInputStream(xmlContent.getBytes(StandardCharsets.UTF_8)));
            doc.getDocumentElement().normalize();

            // 提取解析结果
            Map<String, String> parsed = new LinkedHashMap<>();
            Element root = doc.getDocumentElement();
            NodeList children = root.getChildNodes();
            for (int i = 0; i < children.getLength(); i++) {
                if (children.item(i) instanceof Element) {
                    Element el = (Element) children.item(i);
                    parsed.put(el.getTagName(), el.getTextContent());
                }
            }

            result.put("success", true);
            result.put("message", "Configuration parsed successfully.");
            result.put("parsed", parsed);

            // 检查是否包含 flag 文件内容
            for (String value : parsed.values()) {
                if (value != null && value.contains(FLAG_FILE_CONTENT)) {
                    String flag = flagService.getFlag("xxe", "level1");
                    result.put("flag", flag);
                    break;
                }
            }

        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "XML parsing error: " + e.getMessage());
        }

        return result;
    }

    /**
     * 自定义 EntityResolver：拦截对 flag 虚拟文件的访问
     */
    private static class FlagEntityResolver implements EntityResolver {
        @Override
        public InputSource resolveEntity(String publicId, String systemId) {
            // 只拦截 flag 虚拟文件，返回模拟内容
            if (systemId != null && systemId.contains("/flag/xxe-level1")) {
                return new InputSource(new StringReader(FLAG_FILE_CONTENT));
            }
            // 其他请求返回 null，走默认解析（真实读取文件）
            return null;
        }
    }
}
