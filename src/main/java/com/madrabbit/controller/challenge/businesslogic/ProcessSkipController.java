package com.madrabbit.controller.challenge.businesslogic;

import com.madrabbit.service.FlagService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/challenge/biz-logic/order")
public class ProcessSkipController {

    @Autowired
    private FlagService flagService;

    // 内存订单存储
    private static final Map<String, Map<String, Object>> orderStore = new ConcurrentHashMap<>();

    // 商品数据
    private static final Map<Integer, Map<String, Object>> PRODUCTS = new LinkedHashMap<>();
    static {
        Map<String, Object> p1 = new LinkedHashMap<>();
        p1.put("id", 1);
        p1.put("name", "Premium Membership");
        p1.put("price", 99.99);
        PRODUCTS.put(1, p1);

        Map<String, Object> p2 = new LinkedHashMap<>();
        p2.put("id", 2);
        p2.put("name", "VIP Package");
        p2.put("price", 199.99);
        PRODUCTS.put(2, p2);
    }

    /**
     * 获取商品列表
     * GET /api/challenge/biz-logic/order/products
     */
    @GetMapping("/products")
    public Map<String, Object> getProducts() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("products", PRODUCTS.values());
        return result;
    }

    /**
     * 步骤1：创建订单
     * POST /api/challenge/biz-logic/order/create
     * Body: { "productId": 1 }
     */
    @PostMapping("/create")
    public Map<String, Object> createOrder(@RequestBody Map<String, Object> request) {
        Map<String, Object> result = new LinkedHashMap<>();

        Integer productId = null;
        try {
            productId = ((Number) request.get("productId")).intValue();
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "Invalid productId");
            return result;
        }

        Map<String, Object> product = PRODUCTS.get(productId);
        if (product == null) {
            result.put("success", false);
            result.put("message", "Product not found");
            return result;
        }

        // 更新关卡状态
        try {
            Map<String, Object> status = flagService.getStatus("business-logic", "level3");
            if (status != null && "未开始".equals(status.get("status"))) {
                flagService.updateStatus("business-logic", "level3", "进行中");
            }
        } catch (Exception e) {
            // 忽略
        }

        String orderId = "ORD-" + System.currentTimeMillis();

        Map<String, Object> order = new LinkedHashMap<>();
        order.put("orderId", orderId);
        order.put("productId", productId);
        order.put("productName", product.get("name"));
        order.put("price", product.get("price"));
        order.put("status", "pending_payment");
        order.put("paid", false);
        order.put("createTime", System.currentTimeMillis());

        orderStore.put(orderId, order);

        result.put("success", true);
        result.put("message", "Order created. Proceed to payment.");
        result.put("orderId", orderId);
        result.put("productName", product.get("name"));
        result.put("price", product.get("price"));
        result.put("status", "pending_payment");
        return result;
    }

    /**
     * 步骤2：支付订单
     * POST /api/challenge/biz-logic/order/pay
     * Body: { "orderId": "ORD-xxx", "amount": 99.99 }
     */
    @PostMapping("/pay")
    public Map<String, Object> payOrder(@RequestBody Map<String, Object> request) {
        Map<String, Object> result = new LinkedHashMap<>();

        String orderId = (String) request.get("orderId");
        Double amount = null;
        try {
            amount = ((Number) request.get("amount")).doubleValue();
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "Invalid amount");
            return result;
        }

        if (orderId == null || orderId.trim().isEmpty()) {
            result.put("success", false);
            result.put("message", "Missing orderId");
            return result;
        }

        Map<String, Object> order = orderStore.get(orderId);
        if (order == null) {
            result.put("success", false);
            result.put("message", "Order not found");
            return result;
        }

        Double requiredPrice = ((Number) order.get("price")).doubleValue();
        if (amount < requiredPrice) {
            result.put("success", false);
            result.put("message", "Insufficient payment. Required: $" + requiredPrice);
            return result;
        }

        order.put("paid", true);
        order.put("status", "paid");
        order.put("paidAmount", amount);
        order.put("payTime", System.currentTimeMillis());

        result.put("success", true);
        result.put("message", "Payment successful. You may now confirm your order.");
        result.put("orderId", orderId);
        result.put("status", "paid");
        result.put("paidAmount", amount);
        return result;
    }

    /**
     * 步骤3：确认订单
     * POST /api/challenge/biz-logic/order/confirm
     * Body: { "orderId": "ORD-xxx" }
     *
     * 【漏洞】不检查 paid 状态，未支付即可确认 → 返回 flag
     */
    @PostMapping("/confirm")
    public Map<String, Object> confirmOrder(@RequestBody Map<String, Object> request) {
        Map<String, Object> result = new LinkedHashMap<>();

        String orderId = (String) request.get("orderId");
        if (orderId == null || orderId.trim().isEmpty()) {
            result.put("success", false);
            result.put("message", "Missing orderId");
            return result;
        }

        Map<String, Object> order = orderStore.get(orderId);
        if (order == null) {
            result.put("success", false);
            result.put("message", "Order not found. Create an order first.");
            return result;
        }

        Boolean isPaid = (Boolean) order.get("paid");

        if (!isPaid) {
            // 漏洞触发：未支付却确认成功
            String flag = flagService.getFlag("business-logic", "level3");
            order.put("status", "confirmed");

            result.put("success", true);
            result.put("message", "Order confirmed successfully! Thank you for your purchase.");
            result.put("orderId", orderId);
            result.put("productName", order.get("productName"));
            result.put("price", order.get("price"));
            result.put("paid", false);
            result.put("status", "confirmed");
            result.put("flag", flag);
        } else {
            // 正常流程
            order.put("status", "confirmed");
            order.put("confirmTime", System.currentTimeMillis());

            result.put("success", true);
            result.put("message", "Order confirmed successfully! Thank you for your purchase.");
            result.put("orderId", orderId);
            result.put("productName", order.get("productName"));
            result.put("price", order.get("price"));
            result.put("paid", true);
            result.put("status", "confirmed");
        }

        return result;
    }

    /**
     * 查询订单状态
     * GET /api/challenge/biz-logic/order/status?orderId=xxx
     */
    @GetMapping("/status")
    public Map<String, Object> getOrderStatus(@RequestParam String orderId) {
        Map<String, Object> result = new LinkedHashMap<>();

        Map<String, Object> order = orderStore.get(orderId);
        if (order == null) {
            result.put("success", false);
            result.put("message", "Order not found");
            return result;
        }

        result.put("success", true);
        result.put("order", order);
        return result;
    }
}
