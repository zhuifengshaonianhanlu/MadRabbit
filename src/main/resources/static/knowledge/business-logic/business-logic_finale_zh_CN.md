# 🎓 业务逻辑漏洞 - 结业总结

恭喜你完成了业务逻辑漏洞模块的所有关卡！让我们回顾这段旅程。

---

## 📚 学习路径回顾

### 价格篡改
**核心知识点：**
- 客户端传递的价格参数不可信
- 后端必须根据商品 ID 重新查询价格
- 拦截请求、修改金额参数即可利用

**攻击路径**：
```
1. 拦截购买请求
2. 修改价格参数（改为 0 或负数）
3. 提交修改后的请求
```

**防护建议**：
```python
def process_order(request):
    # 永远不要信任客户端的价格
    product = get_product(request.product_id)
    # 服务端查询真实价格
    total = product.price * request.quantity
    return create_order(total)
```

---

### 优惠券滥用
**核心知识点：**
- 前端单选限制不等于后端限制
- 请求数据结构（数组）暴露了攻击面
- 后端未对数组去重、未限制总折扣

**攻击路径**：
```
1. 观察正常请求结构：{"couponCodes": ["CODE"]}
2. 发现 couponCodes 是数组
3. 构造重复元素使总折扣 >= 原价
4. 提交：{"couponCodes": ["VIP50","VIP50","VIP50",...]}
```

**防护建议**：
```python
def apply_coupons(order, coupon_codes):
    # 去重
    unique_codes = set(coupon_codes)

    # 限制数量
    if len(unique_codes) > 1:
        raise Error("每单只能使用一张优惠券")

    # 校验总折扣不超过原价
    total_discount = sum(get_discount(c) for c in unique_codes)
    if total_discount >= order.price:
        raise Error("折扣金额异常")
```

---

### 流程跳过
**核心知识点：**
- 多步骤流程的每一步接口独立存在
- 前端向导流程只是 UI 层面的限制
- 后端未校验前置步骤的完成状态

**攻击路径**：
```
1. 创建订单拿到 orderId
2. 跳过支付步骤
3. 直接调用确认接口：POST /confirm {"orderId": "..."}
4. 后端未检查 paid 状态，确认成功
```

**防护建议**：
```python
def confirm_order(order_id):
    order = Order.query.get(order_id)
    # 必须验证前置状态
    if order.status != 'paid':
        raise Error("订单未支付，无法确认")
    order.status = 'confirmed'
```

---

## 🛡️ 业务逻辑安全最佳实践

### 1. 服务端全面验证
```
原则：
- 永远不信任客户端数据
- 所有计算在服务端完成
- 所有状态由服务端管理
- 前端限制仅作为用户体验，不作为安全防线
```

### 2. 输入校验与去重
```python
def validate_array_input(items):
    # 去重
    if len(items) != len(set(items)):
        raise Error("Duplicate items detected")
    # 数量限制
    if len(items) > MAX_ALLOWED:
        raise Error("Too many items")
```

### 3. 流程完整性检查
```python
class OrderStateMachine:
    VALID_TRANSITIONS = {
        'created': ['pending_payment'],
        'pending_payment': ['paid', 'cancelled'],
        'paid': ['confirmed'],
        'confirmed': ['shipped'],
    }

    def transition(self, from_state, to_state):
        if to_state not in self.VALID_TRANSITIONS.get(from_state, []):
            raise InvalidTransition()
```

### 4. 总量边界校验
```python
def calculate_final_price(original_price, discounts):
    total_discount = sum(discounts)
    # 折扣不能超过原价
    total_discount = min(total_discount, original_price)
    return original_price - total_discount
```

---

## 💡 业务逻辑 vs 技术漏洞

| 特性 | 业务逻辑漏洞 | 技术漏洞 |
|------|-------------|---------|
| 扫描器检测 | 困难 | 容易 |
| 利用复杂度 | 低（改参数/跳步） | 中-高 |
| 修复方式 | 业务规则调整 | 代码修复 |
| 影响范围 | 直接经济损失 | 系统安全 |
| 测试方法 | 人工分析 | 自动化扫描 |

---

## 🎯 安全开发检查清单

- [ ] 所有价格/金额在服务端重新计算
- [ ] 数组参数做去重和数量限制
- [ ] 每个接口独立验证前置条件
- [ ] 折扣总额不超过原价
- [ ] 流程状态转换使用状态机管理
- [ ] 前端限制不作为唯一安全屏障

---

## 🎊 结语

业务逻辑漏洞的共同特征：**前端有限制，后端没校验**。

你已经掌握了：
- ✅ 通过篡改请求参数利用价格验证缺失
- ✅ 通过观察数据结构发现数组注入攻击面
- ✅ 通过直接调用接口绕过前端流程控制

**记住：每一个前端限制背后，都要问一句 — 后端是否也做了同样的验证？**
