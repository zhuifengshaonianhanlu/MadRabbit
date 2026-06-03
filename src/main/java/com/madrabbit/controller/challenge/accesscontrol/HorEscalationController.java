package com.madrabbit.controller.challenge.accesscontrol;

import com.madrabbit.entity.User;
import com.madrabbit.service.FlagService;
import com.madrabbit.service.UserService;
import com.madrabbit.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import java.util.*;

/**
 * 水平越权漏洞关卡控制器
 * 场景：商家中心订单管理，每个用户(商家)只能看到自己的订单
 * 漏洞：解密接口未验证订单归属，可查看其他商家的订单明文（IDOR）
 */
@RestController
@RequestMapping("/api/challenge/access/hor")
public class HorEscalationController {

    @Autowired
    private FlagService flagService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserService userService;

    @PostConstruct
    public void initOrders() {
        try {
            // 创建表
            jdbcTemplate.execute(
                "CREATE TABLE IF NOT EXISTS merchant_orders (" +
                "id BIGINT AUTO_INCREMENT PRIMARY KEY," +
                "order_no VARCHAR(20) NOT NULL UNIQUE COMMENT '订单编号(YYYYMMDD+5位随机)'," +
                "merchant_id BIGINT NOT NULL COMMENT '商家ID'," +
                "order_time DATETIME NOT NULL COMMENT '订单时间'," +
                "product_name VARCHAR(100) NOT NULL COMMENT '商品名称'," +
                "price DECIMAL(10,2) NOT NULL COMMENT '价格'," +
                "recipient_name VARCHAR(50) NOT NULL COMMENT '收件人姓名'," +
                "recipient_phone VARCHAR(20) NOT NULL COMMENT '收件人手机号'," +
                "recipient_address VARCHAR(200) NOT NULL COMMENT '收件人地址'," +
                "INDEX idx_merchant_id (merchant_id)," +
                "INDEX idx_order_no (order_no)" +
                ") COMMENT '商家订单表'"
            );

            // 检查是否已有数据
            Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM merchant_orders", Integer.class);
            if (count != null && count > 0) {
                return;
            }

            // 动态获取所有活跃用户的 ID（admin, jack, lucy, tom, lili, hanmeimei）
            List<Map<String, Object>> users = jdbcTemplate.queryForList(
                "SELECT id, username FROM users WHERE status = 'ACTIVE' ORDER BY id ASC LIMIT 6");
            if (users.size() < 2) {
                System.err.println("[HorEscalationController] Need at least 2 users to seed orders, found " + users.size());
                return;
            }

            String insertSql = "INSERT INTO merchant_orders (order_no, merchant_id, order_time, product_name, price, recipient_name, recipient_phone, recipient_address) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

            // 为每个用户生成4条订单数据
            String[][] orderData = {
                // admin 的订单
                {"2026050112345", "2026-05-01 09:15:00", "企业级路由器", "2899.00", "王建国", "13900001111", "北京市朝阳区望京SOHO T1"},
                {"2026050356789", "2026-05-03 14:30:00", "服务器硬盘4TB", "1599.00", "李明辉", "13900002222", "北京市海淀区中关村软件园"},
                {"2026050623456", "2026-05-06 10:45:00", "网络交换机", "3299.00", "赵德华", "13900003333", "北京市西城区金融街甲9号"},
                {"2026050978123", "2026-05-09 16:20:00", "机柜42U", "4500.00", "孙志强", "13900004444", "北京市大兴区亦庄经济开发区"},
                // jack 的订单
                {"2026050234561", "2026-05-02 11:20:00", "蓝牙耳机Pro", "599.00", "张小龙", "13811112222", "上海市浦东新区张江高科技园"},
                {"2026050467892", "2026-05-04 15:40:00", "机械键盘红轴", "459.00", "陈思远", "13811113333", "上海市徐汇区漕河泾开发区"},
                {"2026050712345", "2026-05-07 09:00:00", "电竞显示器27寸", "2199.00", "黄嘉豪", "13811114444", "上海市静安区南京西路1601号"},
                {"2026051045678", "2026-05-10 13:25:00", "无线充电板", "129.00", "周文婷", "13811115555", "上海市黄浦区淮海中路333号"},
                // lucy 的订单
                {"2026050345672", "2026-05-03 08:30:00", "瑜伽垫加厚款", "168.00", "林小美", "13722221111", "广州市天河区珠江新城花城大道"},
                {"2026050578901", "2026-05-05 12:15:00", "运动水壶1L", "89.00", "吴丽华", "13722222222", "广州市越秀区北京路步行街"},
                {"2026050812346", "2026-05-08 17:50:00", "跑步手表GPS", "1399.00", "王晓芳", "13722223333", "广州市番禺区万博CBD"},
                {"2026051134567", "2026-05-11 10:10:00", "健身弹力带套装", "59.00", "郑美玲", "13722224444", "广州市白云区白云大道北"},
                // tom 的订单
                {"2026050456783", "2026-05-04 10:00:00", "编程书籍套装", "299.00", "刘学文", "13633331111", "深圳市南山区科技园南路"},
                {"2026050689012", "2026-05-06 14:30:00", "人体工学椅", "1899.00", "杨志豪", "13633332222", "深圳市福田区华强北路"},
                {"2026050923457", "2026-05-09 11:45:00", "USB扩展坞", "259.00", "马瑞霖", "13633333333", "深圳市宝安区前海自贸区"},
                {"2026051256780", "2026-05-12 16:00:00", "降噪耳机头戴式", "899.00", "徐浩然", "13633334444", "深圳市龙岗区坂田街道"},
                // lili 的订单
                {"2026050567894", "2026-05-05 09:30:00", "手工皂礼盒", "128.00", "陈小玲", "13544441111", "杭州市西湖区文三路"},
                {"2026050789013", "2026-05-07 13:00:00", "香薰蜡烛套装", "199.00", "赵雅琪", "13544442222", "杭州市余杭区未来科技城"},
                {"2026051012348", "2026-05-10 08:20:00", "真丝眼罩", "79.00", "孙梦瑶", "13544443333", "杭州市滨江区网商路"},
                {"2026051345679", "2026-05-13 15:40:00", "桌面加湿器", "149.00", "王思雨", "13544444444", "杭州市拱墅区大关路"},
                // hanmeimei 的订单
                {"2026050678905", "2026-05-06 11:00:00", "绘画颜料套装", "239.00", "胡艺文", "13455551111", "成都市高新区天府软件园"},
                {"2026050890124", "2026-05-08 14:15:00", "数位板手写板", "699.00", "郭小芳", "13455552222", "成都市武侯区科华北路"},
                {"2026051123459", "2026-05-11 09:50:00", "素描本A3", "45.00", "钱伟杰", "13455553333", "成都市锦江区春熙路"},
                {"2026051456782", "2026-05-14 17:30:00", "马克笔60色", "189.00", "冯晓燕", "13455554444", "成都市青羊区宽窄巷子"}
            };

            int ordersPerUser = 4;
            for (int i = 0; i < users.size() && i * ordersPerUser < orderData.length; i++) {
                long merchantId = ((Number) users.get(i).get("id")).longValue();
                for (int j = 0; j < ordersPerUser; j++) {
                    int idx = i * ordersPerUser + j;
                    if (idx >= orderData.length) break;
                    String[] d = orderData[idx];
                    jdbcTemplate.update(insertSql, d[0], merchantId, d[1], d[2],
                        Double.parseDouble(d[3]), d[4], d[5], d[6]);
                }
            }

            System.out.println("[HorEscalationController] Initialized merchant_orders with " + Math.min(users.size() * ordersPerUser, orderData.length) + " orders for " + users.size() + " users");
        } catch (Exception e) {
            System.err.println("[HorEscalationController] Failed to initialize merchant_orders: " + e.getMessage());
        }
    }

    /**
     * 从请求中解析当前登录用户的ID
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
                // token 解析失败，fallback
            }
        }
        // fallback: 未登录或解析失败，默认返回 null
        return null;
    }

    /**
     * 获取当前商家的订单列表（脱敏）
     * GET /api/challenge/access/hor/orders
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
     * 解密订单收件人信息
     * POST /api/challenge/access/hor/order/decrypt
     *
     * 【漏洞】未校验 orderNo 是否属于当前商家，直接返回明文
     */
    @PostMapping("/order/decrypt")
    public Map<String, Object> decryptOrder(@RequestBody Map<String, String> body, HttpServletRequest request) {
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
                "SELECT merchant_id, recipient_name, recipient_phone, recipient_address FROM merchant_orders WHERE order_no = ?",
                orderNo.trim()
            );

            if (rows.isEmpty()) {
                result.put("success", false);
                result.put("message", "订单不存在");
                return result;
            }

            Map<String, Object> order = rows.get(0);
            long orderMerchantId = ((Number) order.get("merchant_id")).longValue();

            // 【漏洞核心】未验证订单是否属于当前商家，直接返回明文
            result.put("success", true);
            result.put("orderNo", orderNo.trim());
            result.put("recipientName", order.get("recipient_name"));
            result.put("recipientPhone", order.get("recipient_phone"));
            result.put("recipientAddress", order.get("recipient_address"));

            // 检测越权：订单不属于当前商家
            if (orderMerchantId != merchantId) {
                String flag = flagService.getFlag("access-control", "level1");
                result.put("flag", flag);
                result.put("message", "越权访问成功！你查看了其他商家的订单信息。");
            }
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "解密失败: " + e.getMessage());
        }
        return result;
    }

    // 姓名脱敏：保留第一个字，后面用 **
    private String maskName(String name) {
        if (name == null || name.length() <= 1) return "****";
        return name.charAt(0) + "**";
    }

    // 手机号脱敏：保留前3位和后4位
    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) return "****";
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }

    // 地址脱敏：保留前3个字（城市），后面用 ****
    private String maskAddress(String address) {
        if (address == null || address.length() <= 3) return "****";
        return address.substring(0, 3) + "****";
    }
}
