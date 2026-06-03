# 🎓 SSRF 服务端请求伪造 - 结业总结

恭喜你完成了 SSRF 服务端请求伪造模块的所有关卡！你已经学会了如何"借刀杀人"——让服务器成为你的攻击代理。让我们回顾这段旅程。

---

## 📚 学习路径回顾

### 第0关：获取秘籍 - 理论基础
你了解了 SSRF 的本质：
- **SSRF 的核心**：控制服务器发起的请求
- **攻击价值**：突破网络边界，访问内网资源
- **常见入口**：URL 参数、文件导入、Webhook 等

---

### 第一关：基础 SSRF - 内网探测
**核心知识点：**
- 利用服务器探测内网拓扑
- 通过响应差异判断主机存活和端口开放
- HTTP 协议的基本 SSRF 利用

**攻击路径**：
```
1. 发现接受 URL 参数的功能
2. 尝试访问内网地址（192.168.x.x, 10.x.x.x）
3. 通过响应判断内网结构
4. 深入探测敏感服务
```

**防护建议**：
```python
# 验证并限制目标 URL
from urllib.parse import urlparse
import socket

def validate_url(url):
    parsed = urlparse(url)
    # 只允许 HTTP/HTTPS
    if parsed.scheme not in ['http', 'https']:
        return False
    # 解析域名获取 IP
    try:
        ip = socket.gethostbyname(parsed.hostname)
    except:
        return False
    # 检查是否为内网 IP
    return not is_internal_ip(ip)
```

---

### 第二关：协议利用
**核心知识点：**
- file:// 协议读取本地文件
- gopher:// 协议构造任意 TCP 请求
- dict:// 协议探测服务

**攻击流程**：
```
1. 测试服务器支持的协议
2. 利用 file:// 读取敏感配置
3. 利用 gopher:// 攻击内网服务（Redis、MySQL）
4. 获取更大权限或数据
```

**防护建议**：
```java
// 只允许 HTTP/HTTPS 协议
URL url = new URL(userInput);
if (!url.getProtocol().matches("https?")) {
    throw new SecurityException("Protocol not allowed");
}
```

---

### 第三关：云环境 SSRF
**核心知识点：**
- 云 Metadata API 的危险性
- 通过 SSRF 获取云凭证
- 横向移动到其他云资源

**攻击流程**：
```
1. 识别目标运行在云环境
2. 访问 169.254.169.254 获取 metadata
3. 提取 IAM 凭证
4. 使用凭证访问其他云服务（S3、RDS 等）
```

**防护建议**：
```
# AWS: 启用 IMDSv2（需要 Token）
# 网络层面阻断对 metadata 的访问
# 最小权限原则配置 IAM 角色
```

---

## 🛡️ SSRF 防护最佳实践

### 1. URL 白名单（最推荐）
```python
ALLOWED_HOSTS = ['api.trusted.com', 'cdn.example.com']

def is_allowed(url):
    parsed = urlparse(url)
    return parsed.hostname in ALLOWED_HOSTS
```

### 2. IP 黑名单检查
```python
import ipaddress

BLOCKED_RANGES = [
    ipaddress.ip_network('10.0.0.0/8'),
    ipaddress.ip_network('172.16.0.0/12'),
    ipaddress.ip_network('192.168.0.0/16'),
    ipaddress.ip_network('127.0.0.0/8'),
    ipaddress.ip_network('169.254.0.0/16'),  # Cloud metadata
]

def is_blocked(ip):
    addr = ipaddress.ip_address(ip)
    return any(addr in network for network in BLOCKED_RANGES)
```

### 3. DNS Rebinding 防护
```
1. 解析域名获取 IP 后进行检查
2. 使用固定的 IP 发起请求（不再解析）
3. 或使用短 TTL 检测机制
```

### 4. 网络隔离
```
将处理外部 URL 的服务放在隔离网络中：
- 无法访问内网其他服务
- 无法访问云 metadata
- 只能访问互联网
```

---

## 💡 三种 SSRF 场景对比

| 特性 | 基础 SSRF | 协议利用 | 云环境 |
|------|----------|----------|--------|
| 攻击目标 | 内网服务 | 本地文件/内网服务 | 云凭证 |
| 危害程度 | 中 | 高 | 极高 |
| 防护重点 | IP 过滤 | 协议限制 | Metadata 保护 |
| 利用难度 | 低 | 中 | 低 |
| 常见场景 | 内网扫描 | 攻击 Redis/MySQL | 云账户接管 |

---

## 🎯 安全开发检查清单

### 输入验证
- [ ] 实现 URL 白名单
- [ ] 检查解析后的 IP 地址
- [ ] 限制允许的协议
- [ ] 防范 DNS Rebinding

### 网络层面
- [ ] 隔离发起外部请求的服务
- [ ] 限制出站网络访问
- [ ] 阻断对 metadata 服务的访问

### 云环境特定
- [ ] 使用 IMDSv2（AWS）
- [ ] 最小权限 IAM 角色
- [ ] 监控异常 metadata 访问

---

## 🚀 WAF 绕过的艺术

攻击者常用的绕过技巧：

| 技巧 | 示例 |
|------|------|
| IP 进制转换 | 127.0.0.1 → 0x7f000001 |
| DNS Rebinding | evil.com → 127.0.0.1 |
| URL 编码 | %31%32%37%2e%30%2e%30%2e%31 |
| IPv6 | http://[::1]/ |
| 重定向 | evil.com 302→ 127.0.0.1 |

**教训**：不要只做黑名单过滤，多层防御才是王道。

---

## 🎊 结语

SSRF 教会我们一个重要教训：**网络边界是脆弱的**。

当你信任用户提供的 URL 时，你实际上是把服务器的网络访问能力交给了用户。在云原生时代，一个 SSRF 漏洞可能意味着整个云账户的沦陷。

你已经掌握了：
✅ SSRF 的原理与内网探测技术  
✅ 协议滥用与本地文件读取  
✅ 云环境 SSRF 与凭证窃取

**SSRF 看似简单，实则威力巨大。在微服务和云原生架构盛行的今天，它的危害只会越来越大。**

---

**[我已经掌握，准备出关]**
