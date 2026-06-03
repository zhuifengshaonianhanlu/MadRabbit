package com.madrabbit.controller.challenge.businesslogic;

import com.madrabbit.service.FlagService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@RestController
@RequestMapping("/api/challenge/biz-logic/coupon")
public class CouponAbuseController {

    @Autowired
    private FlagService flagService;

    private static final BigDecimal ORIGINAL_PRICE = new BigDecimal("299.99");
    private static final String PRODUCT_NAME = "Annual VIP Membership";

    private static final List<Map<String, Object>> COUPONS = new ArrayList<>();
    private static final Map<String, BigDecimal> COUPON_DISCOUNTS = new HashMap<>();

    static {
        Map<String, Object> c1 = new LinkedHashMap<>();
        c1.put("code", "SUMMER30");
        c1.put("discount", 30);
        c1.put("description", "Summer Sale - $30 off");
        COUPONS.add(c1);

        Map<String, Object> c2 = new LinkedHashMap<>();
        c2.put("code", "WELCOME20");
        c2.put("discount", 20);
        c2.put("description", "New User Bonus - $20 off");
        COUPONS.add(c2);

        Map<String, Object> c3 = new LinkedHashMap<>();
        c3.put("code", "VIP50");
        c3.put("discount", 50);
        c3.put("description", "VIP Exclusive - $50 off");
        COUPONS.add(c3);

        COUPON_DISCOUNTS.put("SUMMER30", new BigDecimal("30"));
        COUPON_DISCOUNTS.put("WELCOME20", new BigDecimal("20"));
        COUPON_DISCOUNTS.put("VIP50", new BigDecimal("50"));
    }

    /**
     * 获取订单结算信息 + 可用优惠券列表
     */
    @GetMapping("/checkout")
    public Map<String, Object> getCheckoutInfo() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);

        Map<String, Object> order = new LinkedHashMap<>();
        order.put("product", PRODUCT_NAME);
        order.put("originalPrice", ORIGINAL_PRICE);
        result.put("order", order);

        result.put("coupons", COUPONS);
        result.put("note", "Select one coupon to apply at checkout.");
        return result;
    }

    /**
     * 结算支付 — 漏洞：不对 couponCodes 数组去重或限制数量
     */
    @PostMapping("/pay")
    public Map<String, Object> pay(@RequestBody Map<String, Object> request) {
        Map<String, Object> result = new LinkedHashMap<>();

        @SuppressWarnings("unchecked")
        List<String> couponCodes = (List<String>) request.get("couponCodes");

        if (couponCodes == null) {
            couponCodes = Collections.emptyList();
        }

        // 更新关卡状态为"进行中"
        try {
            Map<String, Object> status = flagService.getStatus("business-logic", "level2");
            if (status != null && "未开始".equals(status.get("status"))) {
                flagService.updateStatus("business-logic", "level2", "进行中");
            }
        } catch (Exception e) {
            // 忽略状态更新异常
        }

        // 【漏洞核心】逐个累加折扣，不去重、不限数量
        BigDecimal totalDiscount = BigDecimal.ZERO;
        List<String> appliedCoupons = new ArrayList<>();

        for (String code : couponCodes) {
            if (code == null) continue;
            String normalized = code.toUpperCase().trim();
            BigDecimal discount = COUPON_DISCOUNTS.get(normalized);
            if (discount != null) {
                totalDiscount = totalDiscount.add(discount);
                appliedCoupons.add(normalized);
            }
        }

        BigDecimal finalPrice = ORIGINAL_PRICE.subtract(totalDiscount);

        result.put("success", true);
        result.put("product", PRODUCT_NAME);
        result.put("originalPrice", ORIGINAL_PRICE);
        result.put("totalDiscount", totalDiscount);
        result.put("finalPrice", finalPrice.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : finalPrice);
        result.put("appliedCoupons", appliedCoupons);

        if (finalPrice.compareTo(BigDecimal.ZERO) <= 0) {
            // 触发漏洞 — 免费拿到商品
            String flag = flagService.getFlag("business-logic", "level2");
            result.put("message", "Payment successful! You got the item for FREE!");
            result.put("flag", flag);
        } else {
            result.put("message", "Payment successful! Amount charged: $" + finalPrice.setScale(2, RoundingMode.HALF_UP));
        }

        return result;
    }
}
