# ⚙️ 实验手册：安全配置缺陷

### 0. 序言
最厉害的锁也挡不住一扇敞开的门。安全配置缺陷就是那扇敞开的门——不是代码写得差，而是根本没配好。

*   **安全配置缺陷**：由于错误的系统、应用或服务配置导致的安全问题。
*   **核心思想**：你的代码可能很安全，但你的配置可能在"裸奔"。

本章目标：**理解为什么"用默认密码"是最常见的入侵方式之一。**

---

### 1. 默认凭据：最简单的入口

**常见默认账户**：
```
admin:admin
admin:123456
root:root
administrator:password
tomcat:tomcat
test:test
```

**高危服务默认凭据**：
```
MySQL: root:(空密码)
Redis: (无密码)
MongoDB: (无认证)
Elasticsearch: (无认证)
Jenkins: admin:admin
```

**冷知识**：
很多企业被黑客入侵，起点就是一个忘记修改的默认密码。

---

### 2. 错误信息泄露

**问题**：详细的错误信息暴露了系统内部结构。

**危险示例**：
```
java.sql.SQLException: Column 'password' not found
    at com.mysql.jdbc.SQLError.createSQLException(SQLError.java:1073)
    at com.myapp.dao.UserDAO.findByUsername(UserDAO.java:42)
```

泄露了什么？
- 数据库类型（MySQL）
- 表结构（有 password 列）
- 代码结构（UserDAO.java）
- 行号（42行）

**修复方法**：
```
生产环境：
- 显示通用错误信息："系统错误，请稍后重试"
- 详细错误记录到日志，不显示给用户
- 关闭 debug 模式
```

---

### 3. HTTP 安全头

**Content-Security-Policy（CSP）**：
```http
Content-Security-Policy: default-src 'self'; script-src 'self'
```
限制资源加载来源，防止 XSS。

**X-Frame-Options**：
```http
X-Frame-Options: DENY
```
防止页面被嵌入 iframe，防止点击劫持。

**Strict-Transport-Security（HSTS）**：
```http
Strict-Transport-Security: max-age=31536000; includeSubDomains
```
强制使用 HTTPS。

**X-Content-Type-Options**：
```http
X-Content-Type-Options: nosniff
```
防止 MIME 类型嗅探。

**X-XSS-Protection**（已弃用但仍有用）：
```http
X-XSS-Protection: 1; mode=block
```

---

### 4. 不安全的 HTTP 方法

**危险方法**：
```http
PUT /shell.jsp HTTP/1.1
# 直接上传文件到服务器

DELETE /important-file HTTP/1.1
# 删除服务器文件

TRACE /test HTTP/1.1
# 可能泄露 Cookie（XST 攻击）
```

**检测方法**：
```http
OPTIONS / HTTP/1.1
Host: target.com

# 响应
Allow: GET, HEAD, POST, PUT, DELETE, OPTIONS
```

**修复**：
只允许必要的 HTTP 方法，禁用 PUT、DELETE、TRACE 等。

---

### 5. 常见配置问题清单

**目录列表开启**：
```
访问 /images/ 能看到所有文件列表
```

**敏感文件暴露**：
```
/.git/              # Git 仓库
/.svn/              # SVN 仓库
/.env               # 环境变量
/web.config         # IIS 配置
/phpinfo.php        # PHP 信息
/server-status      # Apache 状态
/.htaccess          # Apache 配置
/backup.sql         # 数据库备份
```

**调试功能开启**：
```
Spring Boot Actuator: /actuator/env
Swagger/OpenAPI: /v3/api-docs, /swagger-ui/index.html
Django Debug: 详细错误页面
```

---

### 6. 防御措施

**安全配置检查清单**：
```
□ 修改所有默认密码
□ 禁用不必要的服务和端口
□ 关闭目录列表
□ 配置适当的 HTTP 安全头
□ 生产环境禁用调试模式
□ 定期扫描敏感文件暴露
□ 最小权限原则配置账户
□ 启用访问日志和监控
```

**自动化扫描**：
```bash
# 使用 nmap 扫描开放端口
nmap -sV target.com

# 使用 nikto 扫描 Web 配置问题
nikto -h http://target.com
```

---

### 7. 结语

安全配置缺陷是最容易犯也最容易被利用的漏洞。它不需要高深的技术，只需要攻击者尝试一下默认密码，或者访问一下 `/.git/`。

记住：**安全是一个持续的过程。上线只是开始，配置加固永远在路上。**

---

**[准备好了吗？去检查一下你的配置是否"裸奔"吧]**
