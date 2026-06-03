package com.madrabbit.controller.challenge.businesslogic;

import com.madrabbit.service.FlagService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/challenge/biz-logic/price")
public class PriceTamperController {

    @Autowired
    private FlagService flagService;

    // 模拟商品数据
    private static final List<Map<String, Object>> PRODUCTS = new ArrayList<>();
    static {
        Map<String, Object> p1 = new HashMap<>(); 
        p1.put("id", 1); 
        p1.put("name", "Laptop"); 
        p1.put("price", 999.99);
        
        Map<String, Object> p2 = new HashMap<>(); 
        p2.put("id", 2); 
        p2.put("name", "Phone"); 
        p2.put("price", 699.99);
        
        Map<String, Object> p3 = new HashMap<>(); 
        p3.put("id", 3); 
        p3.put("name", "Tablet"); 
        p3.put("price", 499.99);
        
        PRODUCTS.add(p1); PRODUCTS.add(p2); PRODUCTS.add(p3);
    }

    /**
     * 获取商品列表
     * GET /api/challenge/biz-logic/price/products
     */
    @GetMapping("/products")
    public Map<String, Object> getProducts() {
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("products", PRODUCTS);
        return result;
    }

    /**
     * 下单接口 - 存在价格篡改漏洞
     * POST /api/challenge/biz-logic/price/order
     * Body: { "productId": 1, "productName": "Laptop", "price": 999.99, "quantity": 1 }
     *
     * 漏洞点：服务端直接使用前端传来的 price 计算订单金额，未与数据库中的真实价格做校验。
     * 当用户通过拦截请求修改 price 为低于真实价格的值时，返回 flag。
     */
    @PostMapping("/order")
    public Map<String, Object> createOrder(@RequestBody Map<String, Object> request) {
        Map<String, Object> result = new LinkedHashMap<>();

        Integer productId = null;
        Double submittedPrice = null;
        Integer quantity = 1;

        try {
            productId = ((Number) request.get("productId")).intValue();
            submittedPrice = ((Number) request.get("price")).doubleValue();
            if (request.get("quantity") != null) {
                quantity = ((Number) request.get("quantity")).intValue();
            }
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "Invalid request parameters");
            return result;
        }

        if (productId == null || submittedPrice == null) {
            result.put("success", false);
            result.put("message", "Missing required parameters");
            return result;
        }

        // 查找商品真实价格
        Map<String, Object> targetProduct = null;
        for (Map<String, Object> product : PRODUCTS) {
            if (product.get("id").equals(productId)) {
                targetProduct = product;
                break;
            }
        }

        if (targetProduct == null) {
            result.put("success", false);
            result.put("message", "Product not found");
            return result;
        }

        Double actualPrice = ((Number) targetProduct.get("price")).doubleValue();
        String productName = (String) targetProduct.get("name");

        // 更新关卡状态为"进行中"
        try {
            Map<String, Object> status = flagService.getStatus("business-logic", "level1");
            if (status != null && "未开始".equals(status.get("status"))) {
                flagService.updateStatus("business-logic", "level1", "进行中");
            }
        } catch (Exception e) {
            // 忽略状态更新异常
        }

        // 【漏洞核心】服务端直接使用前端提交的价格，未做校验
        // 当提交价格低于真实价格时，说明用户已成功篡改价格
        if (submittedPrice < actualPrice) {
            String flag = flagService.getFlag("business-logic", "level1");
            result.put("success", true);
            result.put("orderId", "ORD-" + System.currentTimeMillis());
            result.put("productName", productName);
            result.put("quantity", quantity);
            result.put("paidPrice", submittedPrice);
            result.put("originalPrice", actualPrice);
            result.put("message", "Order placed! You paid $" + String.format("%.2f", submittedPrice) + " for " + productName + " (original: $" + String.format("%.2f", actualPrice) + ")");
            result.put("flag", flag);
        } else {
            result.put("success", true);
            result.put("orderId", "ORD-" + System.currentTimeMillis());
            result.put("productName", productName);
            result.put("quantity", quantity);
            result.put("totalPrice", submittedPrice * quantity);
            result.put("message", "Order placed successfully!");
        }

        return result;
    }
}
