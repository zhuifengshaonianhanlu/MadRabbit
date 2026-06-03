# 🚪 实验手册：访问控制漏洞

### 0. 序言
登录成功就能为所欲为？天真了。访问控制是决定"谁能做什么"的最后一道防线，而这道防线经常被开发者忽视。

*   **访问控制（Access Control）**：决定已认证用户能访问哪些资源、执行哪些操作的机制。
*   **核心思想**：你是你，但你不能变成别人；你是用户，但你不是管理员。

本章目标：**理解为什么"改一个ID"就能看到别人的数据。**

---

### 1. 水平越权 vs 垂直越权

**水平越权（Horizontal Privilege Escalation）**：
```
用户 A 访问用户 B 的资源
例：/user/profile?id=1001 → /user/profile?id=1002
同级别用户之间的越权
```

**垂直越权（Vertical Privilege Escalation）**：
```
普通用户访问管理员功能
例：普通用户访问 /admin/users
低权限用户获取高权限功能
```

**记忆口诀**：
- 水平越权：平级之间串门
- 垂直越权：普通人闯入 VIP 区

---

### 2. IDOR - 不安全的直接对象引用

**什么是 IDOR**：
Insecure Direct Object Reference（不安全的直接对象引用），通过修改请求中的标识符访问未授权资源。

**经典示例**：
```
# 查看自己的订单
GET /api/order/12345

# 修改 ID 查看他人订单
GET /api/order/12346
GET /api/order/12347

# 如果服务器不验证归属，攻击成功！
```

**常见 IDOR 场景**：
- 订单详情、用户资料、私信内容
- 文件下载：`/download?file=report_1001.pdf`
- 数据导出：`/export?userId=1001`

---

### 3. 常见访问控制缺陷

**仅依赖前端控制**：
```javascript
// 前端隐藏管理员按钮
if (!user.isAdmin) {
    adminButton.style.display = 'none';
}
// 但后端 API 没有权限验证！
```

**基于路径的粗粒度控制**：
```
/admin/* → 需要管理员权限
/api/user/* → 需要登录

# 但 /api/admin/users 呢？
```

**参数可预测**：
```
用户 ID 使用递增数字：1001, 1002, 1003...
订单号使用时间戳：20240101001, 20240101002...
```

---

### 4. RBAC vs ABAC

**RBAC（基于角色的访问控制）**：
```
用户 → 角色 → 权限
例：张三 → 经理 → [查看报表, 审批请假]
```

**ABAC（基于属性的访问控制）**：
```
根据用户属性、资源属性、环境条件动态决策
例：部门=销售 AND 级别>=5 AND 时间=工作时间 → 允许访问客户数据
```

**选择建议**：
- 简单场景：RBAC 足够
- 复杂场景：ABAC 更灵活

---

### 5. 实际漏洞案例

**案例1：修改用户ID查看他人信息**
```
POST /api/user/profile
{"userId": 1001}  →  {"userId": 1002}
```

**案例2：越权修改他人密码**
```
POST /api/user/changePassword
{"userId": 1001, "newPassword": "hacked"}
```

**案例3：普通用户访问管理功能**
```
# 前端隐藏了入口，但直接访问 API
GET /api/admin/getAllUsers
```

---

### 6. 防御措施

**服务端强制验证**：
```python
def get_order(order_id, current_user):
    order = Order.query.get(order_id)
    if order.user_id != current_user.id:
        raise PermissionError("You can only view your own orders")
    return order
```

**使用不可预测的标识符**：
```python
import uuid
order_id = str(uuid.uuid4())  # a1b2c3d4-e5f6-7890-...
```

**最小权限原则**：
```
默认拒绝所有访问
只授予完成任务所需的最小权限
定期审查权限配置
```

**统一的权限检查层**：
```java
@PreAuthorize("hasRole('ADMIN') or #userId == authentication.principal.id")
public User getUser(Long userId) { ... }
```

---

### 7. 结语

访问控制漏洞的可怕之处在于它的隐蔽性——功能看起来完全正常，只是"你能看到不该看的东西"。

记住：**永远在服务端验证当前用户是否有权限访问请求的资源。**

---

**[准备好了吗？去看看别人的秘密吧（在授权范围内）]**
