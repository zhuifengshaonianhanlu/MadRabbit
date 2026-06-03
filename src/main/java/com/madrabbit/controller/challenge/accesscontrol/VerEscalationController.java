package com.madrabbit.controller.challenge.accesscontrol;

import com.madrabbit.entity.User;
import com.madrabbit.service.FlagService;
import com.madrabbit.service.UserService;
import com.madrabbit.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.*;

/**
 * 垂直越权漏洞关卡控制器
 * 场景：商家订单管理 - 仅 ADMIN 可编辑/删除订单
 * 漏洞点在 UserController.updateUser() 未校验角色修改权限，
 * 普通用户可通过篡改请求提升角色为 ADMIN，从而获得订单编辑/删除权限。
 */
@RestController
@RequestMapping("/api/challenge/access/ver")
public class VerEscalationController {

    @Autowired
    private FlagService flagService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserService userService;

    /**
     * 从请求中解析当前登录用户
     */
    private User getCurrentUser(HttpServletRequest request) {
        String authorization = request.getHeader("Authorization");
        if (authorization != null && authorization.startsWith("Bearer ")) {
            String token = authorization.substring(7);
            try {
                String username = jwtUtil.getUsernameFromToken(token);
                if (username != null) {
                    return userService.findByUsername(username);
                }
            } catch (Exception e) {
                // token 解析失败
            }
        }
        return null;
    }

    /**
     * 获取当前商家的订单列表
     * GET /api/challenge/access/ver/orders
     * 返回订单数据 + 当前用户角色（前端据此决定是否渲染编辑/删除按钮）
     */
    @GetMapping("/orders")
    public Map<String, Object> getOrders(HttpServletRequest request) {
        Map<String, Object> result = new HashMap<>();

        User currentUser = getCurrentUser(request);
        if (currentUser == null) {
            result.put("success", false);
            result.put("message", "未登录或登录已过期");
            return result;
        }

        try {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT order_no, order_time, product_name, price, recipient_name, recipient_phone, recipient_address FROM merchant_orders WHERE merchant_id = ? ORDER BY order_time DESC",
                currentUser.getId()
            );

            List<Map<String, Object>> orders = new ArrayList<>();
            for (Map<String, Object> row : rows) {
                Map<String, Object> order = new LinkedHashMap<>();
                order.put("orderNo", row.get("order_no"));
                order.put("orderTime", row.get("order_time").toString());
                order.put("productName", row.get("product_name"));
                order.put("price", row.get("price"));
                order.put("recipientName", maskName((String) row.get("recipient_name")));
                order.put("recipientPhone", maskPhone((String) row.get("recipient_phone")));
                order.put("recipientAddress", maskAddress((String) row.get("recipient_address")));
                orders.add(order);
            }

            result.put("success", true);
            result.put("merchantId", currentUser.getId());
            result.put("role", currentUser.getRole());
            result.put("orders", orders);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "查询订单失败: " + e.getMessage());
        }
        return result;
    }

    /**
     * 编辑订单（仅 ADMIN）
     * PUT /api/challenge/access/ver/order/{orderNo}
     * Body: { "productName": "xxx", "price": 100.00 }
     */
    @PutMapping("/order/{orderNo}")
    public ResponseEntity<Map<String, Object>> updateOrder(
            @PathVariable String orderNo,
            @RequestBody Map<String, Object> body,
            HttpServletRequest request) {

        Map<String, Object> result = new HashMap<>();

        User currentUser = getCurrentUser(request);
        if (currentUser == null) {
            result.put("success", false);
            result.put("message", "未登录或登录已过期");
            return ResponseEntity.status(401).body(result);
        }

        // 校验角色：仅 ADMIN 可编辑
        if (!"ADMIN".equalsIgnoreCase(currentUser.getRole())) {
            result.put("success", false);
            result.put("message", "权限不足，仅管理员可编辑订单");
            return ResponseEntity.status(403).body(result);
        }

        String productName = (String) body.get("productName");
        Object priceObj = body.get("price");

        if (productName == null || priceObj == null) {
            result.put("success", false);
            result.put("message", "请提供 productName 和 price");
            return ResponseEntity.badRequest().body(result);
        }

        try {
            double price = Double.parseDouble(priceObj.toString());
            int rows = jdbcTemplate.update(
                "UPDATE merchant_orders SET product_name = ?, price = ? WHERE order_no = ? AND merchant_id = ?",
                productName, price, orderNo, currentUser.getId()
            );

            if (rows > 0) {
                String flag = flagService.getFlag("access-control", "level2");
                result.put("success", true);
                result.put("message", "订单编辑成功！你已成功利用垂直越权漏洞。");
                result.put("flag", flag);
            } else {
                result.put("success", false);
                result.put("message", "订单不存在或不属于当前用户");
            }
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "编辑订单失败: " + e.getMessage());
        }
        return ResponseEntity.ok(result);
    }

    /**
     * 删除订单（仅 ADMIN）
     * DELETE /api/challenge/access/ver/order/{orderNo}
     */
    @DeleteMapping("/order/{orderNo}")
    public ResponseEntity<Map<String, Object>> deleteOrder(
            @PathVariable String orderNo,
            HttpServletRequest request) {

        Map<String, Object> result = new HashMap<>();

        User currentUser = getCurrentUser(request);
        if (currentUser == null) {
            result.put("success", false);
            result.put("message", "未登录或登录已过期");
            return ResponseEntity.status(401).body(result);
        }

        // 校验角色：仅 ADMIN 可删除
        if (!"ADMIN".equalsIgnoreCase(currentUser.getRole())) {
            result.put("success", false);
            result.put("message", "权限不足，仅管理员可删除订单");
            return ResponseEntity.status(403).body(result);
        }

        try {
            int rows = jdbcTemplate.update(
                "DELETE FROM merchant_orders WHERE order_no = ? AND merchant_id = ?",
                orderNo, currentUser.getId()
            );

            if (rows > 0) {
                String flag = flagService.getFlag("access-control", "level2");
                result.put("success", true);
                result.put("message", "订单删除成功！你已成功利用垂直越权漏洞。");
                result.put("flag", flag);
            } else {
                result.put("success", false);
                result.put("message", "订单不存在或不属于当前用户");
            }
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "删除订单失败: " + e.getMessage());
        }
        return ResponseEntity.ok(result);
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
