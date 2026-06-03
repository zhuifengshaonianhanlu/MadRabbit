package com.madrabbit.controller.challenge.xss;

import com.madrabbit.service.FlagService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.*;

/**
 * 反射型XSS关卡控制器
 * 模拟商品搜索功能，故意不转义用户输入形成反射型XSS漏洞
 */
@RestController
@RequestMapping("/api/challenge/xss/reflected")
public class ReflectedXssController {

    @Autowired
    private FlagService flagService;

    // 有效 XSS payload 列表（不依赖 script 标签、能通过 innerHTML 执行）
    private static final String[] VALID_PAYLOADS = {
        "\"><img src=x onerror=alert(1)>",
        "\"><svg onload=alert(1)>",
        "\"><input onfocus=alert(1) autofocus>",
        "\"><body onload=alert(1)>",
        "\"><iframe src=javascript:alert(1)>",
        "\"><details open ontoggle=alert(1)>",
        "\" onfocus=alert(1) autofocus=\"",
        "\" onmouseover=alert(1) style=\"",
        "\" onclick=alert(1)>"
    };

    // 模拟商品数据
    private static final List<Map<String, Object>> PRODUCTS = new ArrayList<>();
    static {
        PRODUCTS.add(createProduct(1, "Wireless Mouse", 29.99));
        PRODUCTS.add(createProduct(2, "Mechanical Keyboard", 89.99));
        PRODUCTS.add(createProduct(3, "USB Hub", 19.99));
        PRODUCTS.add(createProduct(4, "Monitor Stand", 45.99));
        PRODUCTS.add(createProduct(5, "Laptop Bag", 35.99));
    }

    private static Map<String, Object> createProduct(int id, String name, double price) {
        Map<String, Object> product = new HashMap<>();
        product.put("id", id);
        product.put("name", name);
        product.put("price", price);
        return product;
    }

    /**
     * 搜索商品 - 故意不转义用户输入，形成反射型XSS漏洞
     * POST /api/challenge/xss/reflected/search
     * Body: { "query": "用户输入" }
     */
    @PostMapping("/search")
    public Map<String, Object> search(@RequestBody Map<String, String> request) {
        Map<String, Object> result = new HashMap<>();
        String query = request.get("query");

        // 1. 输入验证
        if (query == null || query.trim().isEmpty()) {
            result.put("success", false);
            result.put("message", "Query cannot be empty");
            return result;
        }

        // 2. XSS Payload 检测（在过滤前检测原始输入）
        boolean isXssValid = isValidXssPayload(query);

        // 3. 自动状态更新：如果是"未开始"，更新为"进行中"
        if (isXssValid) {
            try {
                Map<String, Object> status = flagService.getStatus("xss", "level1");
                if (status != null && "未开始".equals(status.get("status"))) {
                    flagService.updateStatus("xss", "level1", "进行中");
                }
            } catch (Exception e) {
                // 忽略状态更新异常
            }
        }

        // 4. 正则过滤 script 标签（不区分大小写）
        String filteredQuery = query.replaceAll("(?i)</?script[^>]*>", "");

        // 5. 商品搜索模拟：忽略大小写的子字符串匹配（基于原始 query）
        List<Map<String, Object>> matchedProducts = new ArrayList<>();
        for (Map<String, Object> product : PRODUCTS) {
            String name = (String) product.get("name");
            if (name.toLowerCase().contains(query.toLowerCase())) {
                matchedProducts.add(product);
            }
        }

        // 返回响应
        result.put("success", true);
        result.put("query", filteredQuery);  // 返回过滤后的 query（script 标签已移除）
        result.put("message", "Found " + matchedProducts.size() + " results for your search");
        result.put("results", matchedProducts);

        // 6. XSS Payload 匹配成功时返回 flag
        if (isXssValid) {
            String flag = flagService.getFlag("xss", "level1");
            result.put("flag", flag);
            result.put("xss_valid", true);
        }

        return result;
    }

    /**
     * 检测输入是否匹配有效 XSS payload 列表
     * 使用忽略大小写的精确匹配
     */
    private boolean isValidXssPayload(String input) {
        if (input == null || input.trim().isEmpty()) {
            return false;
        }
        
        String trimmedInput = input.trim();
        
        for (String validPayload : VALID_PAYLOADS) {
            if (trimmedInput.equalsIgnoreCase(validPayload)) {
                return true;
            }
        }
        
        return false;
    }
}
