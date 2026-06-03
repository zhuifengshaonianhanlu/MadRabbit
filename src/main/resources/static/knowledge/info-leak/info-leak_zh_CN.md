# 🔍 实验手册：信息泄露漏洞

### 0. 序言
信息泄露是渗透测试的"第一步"，也是最容易被忽视的漏洞。它不会直接让你控制服务器，但会告诉你如何控制服务器。

*   **信息泄露**：系统无意中暴露了不应该被外部获取的敏感信息。
*   **核心思想**：攻击者不需要猜测，因为你已经把答案写在了墙上。

本章目标：**理解为什么一条 HTML 注释可能就是攻击的起点。**

---

### 1. HTML 注释泄露

**问题**：开发者在 HTML 中留下的注释被浏览器忽略，却被攻击者看到。

**危险示例**：
```html
<!-- TODO: 删除这个测试账号 admin/test123 -->
<!-- DEBUG: API key = sk-xxxxx -->
<!-- 内网地址: http://192.168.1.100:8080 -->
<!-- 旧版接口: /api/v1/users（已弃用但未删除）-->
```

**如何发现**：
```bash
curl -s https://target.com | grep -i "<!--"
```

**教训**：
HTML 注释不是"私密笔记"，所有人都能看到。

---

### 2. API 过度暴露

**问题**：API 返回了比前端需要更多的数据。

**危险示例**：
```json
// 前端只需要用户名和头像
// 但 API 返回了：
{
    "username": "alice",
    "avatar": "...",
    "email": "alice@secret.com",
    "phone": "138xxxxx",
    "password_hash": "5f4dcc3b5aa765d61d8327deb882cf99",
    "internal_id": "emp_12345",
    "department": "财务部"
}
```

**另一个例子 - 枚举漏洞**：
```
GET /api/user/1 → 200 {"username": "admin"}
GET /api/user/2 → 200 {"username": "alice"}
GET /api/user/999 → 404 "User not found"
# 攻击者可以枚举所有用户
```

---

### 3. 备份文件泄露

**常见泄露文件**：
```
/.git/                    # 源代码！
/.svn/                    # 源代码！
/.env                     # 数据库密码、API 密钥
/config.php.bak           # 配置文件备份
/database.sql             # 数据库导出
/backup.zip               # 网站备份
/www.zip                  # 网站打包
/.DS_Store                # Mac 目录结构
/WEB-INF/web.xml          # Java 配置
/robots.txt               # 可能泄露敏感路径
/sitemap.xml              # 网站结构
```

**Git 泄露利用**：
```bash
# 如果 /.git/ 可访问
wget -r https://target.com/.git/
cd target.com
git checkout .
# 现在你有了完整源代码
```

---

### 4. 错误信息泄露

**问题**：详细的错误信息暴露技术栈和内部结构。

**泄露示例**：
```
Stack Trace:
    at com.myapp.controller.UserController.login(UserController.java:42)
    at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
    
Database Error:
    Connection failed: mysql://root:password123@192.168.1.50:3306/prod_db
```

**从错误中获取的信息**：
- 编程语言和框架
- 数据库类型和位置
- 内网 IP 地址
- 文件路径结构
- 可能的用户名/密码

---

### 5. HTTP 响应头泄露

**危险响应头**：
```http
Server: Apache/2.4.41 (Ubuntu)
X-Powered-By: PHP/7.4.3
X-AspNet-Version: 4.0.30319
```

**为什么危险**：
攻击者可以根据版本号查找已知漏洞。

**修复**：
```nginx
# Nginx
server_tokens off;

# Apache
ServerTokens Prod
ServerSignature Off
```

---

### 6. 防御措施

**代码层面**：
```python
# 返回数据时只包含必要字段
def get_user_profile(user_id):
    user = User.query.get(user_id)
    return {
        "username": user.username,
        "avatar": user.avatar
        # 不返回敏感字段
    }
```

**部署层面**：
```
□ 上线前删除所有调试信息和注释
□ 禁止访问 .git/.svn/.env 等敏感目录
□ 配置自定义错误页面
□ 移除响应头中的版本信息
□ 定期扫描敏感文件暴露
```

**最小信息原则**：
只返回完成功能所需的最少信息，不多给一个字段。

---

### 7. 结语

信息泄露看起来"无害"，但它是攻击链的第一环。攻击者通过收集这些碎片信息，拼凑出完整的攻击路径。

记住：**你泄露的每一条信息，都可能成为攻击者的弹药。**

---

**[准备好了吗？去"窥探"一下目标泄露了什么]**
