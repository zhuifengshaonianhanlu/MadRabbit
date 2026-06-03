package com.madrabbit.controller.challenge.accesscontrol;

import com.madrabbit.entity.User;
import com.madrabbit.service.FlagService;
import com.madrabbit.service.UserService;
import com.madrabbit.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.*;

/**
 * IDOR 关卡控制器 — "前端的密码"
 * 场景：商家订单管理（仅脱敏展示，无操作列）
 * 漏洞点：前端源代码中保留了订单解密查询接口逻辑（模拟前端工程泄露后端接口），
 *         该接口未校验订单归属，可越权查看其他商家的订单明文。
 *         当成功查看到其他商家订单时返回 flag。
 *
 * 注意：本关卡使用独立的解密接口路径，与水平越权关卡（/api/challenge/access/hor/...）不复用。
 */
@RestController
@RequestMapping("/api/challenge/access/idor")
public class IdorController {

    @Autowired
    private FlagService flagService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserService userService;

    /**
     * 从请求中解析当前登录用户 ID
     */
    private Long getCurrentMerchantId(HttpServletRequest request) {
        String authorization = request.getHeader("Authorization");
        if (authorization != null && authorization.startsWith("Bearer ")) {
            String token = authorization.substring(7);
            try {
                String username = jwtUtil.getUsernameFromToken(token);
                if (username != null) {
                    User user = userService.findByUsername(username);
                    if (user != null && user.getId() != null) {
                        return user.getId();
                    }
                }
            } catch (Exception e) {
                // token 解析失败
            }
        }
        return null;
    }

    /**
     * 获取当前商家的订单列表（脱敏，无操作列）
     * GET /api/challenge/access/idor/orders
     */
    @GetMapping("/orders")
    public Map<String, Object> getOrders(HttpServletRequest request) {
        Map<String, Object> result = new HashMap<>();

        Long merchantId = getCurrentMerchantId(request);
        if (merchantId == null) {
            result.put("success", false);
            result.put("message", "未登录或登录已过期");
            return result;
        }

        try {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT order_no, order_time, product_name, price, recipient_name, recipient_phone, recipient_address FROM merchant_orders WHERE merchant_id = ? ORDER BY order_time DESC",
                merchantId
            );

            List<Map<String, Object>> maskedOrders = new ArrayList<>();
            for (Map<String, Object> row : rows) {
                Map<String, Object> order = new LinkedHashMap<>();
                order.put("orderNo", row.get("order_no"));
                order.put("orderTime", row.get("order_time").toString());
                order.put("productName", row.get("product_name"));
                order.put("price", row.get("price"));
                order.put("recipientName", maskName((String) row.get("recipient_name")));
                order.put("recipientPhone", maskPhone((String) row.get("recipient_phone")));
                order.put("recipientAddress", maskAddress((String) row.get("recipient_address")));
                maskedOrders.add(order);
            }

            result.put("success", true);
            result.put("merchantId", merchantId);
            result.put("orders", maskedOrders);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "查询订单失败: " + e.getMessage());
        }
        return result;
    }

    /**
     * 解密订单收件人信息（隐藏接口 — 前端源码中泄露）
     * POST /api/challenge/access/idor/order/detail
     *
     * 【漏洞】未校验 orderNo 是否属于当前商家，直接返回明文
     * 当越权查看到其他商家的订单时返回 flag
     */
    @PostMapping("/order/detail")
    public Map<String, Object> getOrderDetail(@RequestBody Map<String, String> body, HttpServletRequest request) {
        Map<String, Object> result = new HashMap<>();
        String orderNo = body.get("orderNo");

        Long merchantId = getCurrentMerchantId(request);
        if (merchantId == null) {
            result.put("success", false);
            result.put("message", "未登录或登录已过期");
            return result;
        }

        if (orderNo == null || orderNo.trim().isEmpty()) {
            result.put("success", false);
            result.put("message", "请提供订单编号");
            return result;
        }

        try {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT merchant_id, order_no, order_time, product_name, price, recipient_name, recipient_phone, recipient_address FROM merchant_orders WHERE order_no = ?",
                orderNo.trim()
            );

            if (rows.isEmpty()) {
                result.put("success", false);
                result.put("message", "订单不存在");
                return result;
            }

            Map<String, Object> order = rows.get(0);
            long orderMerchantId = ((Number) order.get("merchant_id")).longValue();

            // 直接返回明文（未校验归属 — 漏洞核心）
            result.put("success", true);
            result.put("orderNo", order.get("order_no"));
            result.put("orderTime", order.get("order_time").toString());
            result.put("productName", order.get("product_name"));
            result.put("price", order.get("price"));
            result.put("recipientName", order.get("recipient_name"));
            result.put("recipientPhone", order.get("recipient_phone"));
            result.put("recipientAddress", order.get("recipient_address"));

            // 检测越权：订单不属于当前商家
            if (orderMerchantId != merchantId) {
                String flag = flagService.getFlag("access-control", "level3");
                result.put("flag", flag);
                result.put("message", "越权访问成功！你通过隐藏接口查看了其他商家的订单信息。");
            }
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "查询失败: " + e.getMessage());
        }
        return result;
    }

    // 姓名脱敏
    private String maskName(String name) {
        if (name == null || name.length() <= 1) return "****";
        return name.charAt(0) + "**";
    }

    // 手机号脱敏
    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) return "****";
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }

    // 地址脱敏
    private String maskAddress(String address) {
        if (address == null || address.length() <= 3) return "****";
        return address.substring(0, 3) + "****";
    }
}
