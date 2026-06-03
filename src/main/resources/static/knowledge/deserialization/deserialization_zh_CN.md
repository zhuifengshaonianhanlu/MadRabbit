# 🔓 实验手册：反序列化漏洞

### 0. 序言
序列化是为了方便数据传输，反序列化是把数据"变回"对象。但如果反序列化的数据来自攻击者，那你可能在"复活"一只特洛伊木马。

*   **反序列化漏洞**：当应用程序将不可信的序列化数据还原为对象时，可能导致代码执行或其他安全问题。
*   **核心思想**：你以为你在恢复数据，其实你在执行代码。

本章目标：**理解为什么"一段 Base64"可以变成远程代码执行。**

---

### 1. 什么是序列化/反序列化

**序列化**：将对象转换为可存储/传输的格式
```
对象 → JSON/XML/二进制 → 存储/传输
```

**反序列化**：将存储格式还原为对象
```
JSON/XML/二进制 → 对象 → 使用
```

**常见格式**：
- JSON：`{"name": "Alice", "age": 25}`
- XML：`<user><name>Alice</name></user>`
- Java 序列化：二进制格式，以 `AC ED 00 05` 开头
- PHP 序列化：`O:4:"User":1:{s:4:"name";s:5:"Alice";}`
- Python Pickle：二进制格式

---

### 2. JSON 注入与对象篡改

**场景**：应用信任客户端传来的 JSON 数据。

**攻击示例**：
```json
// 正常请求
{"userId": 123, "role": "user"}

// 篡改后
{"userId": 123, "role": "admin", "isAdmin": true}
```

**更危险的情况 - 原型污染（JavaScript）**：
```json
{"__proto__": {"isAdmin": true}}
```
可能污染所有对象的原型！

**防御**：
- 使用白名单验证字段
- 不要直接将 JSON 合并到对象

---

### 3. Base64 编码篡改

**常见场景**：
应用使用 Base64 编码的数据传递状态或配置。

**攻击流程**：
```
1. 拦截请求，发现 Base64 编码的参数
2. 解码：eyJ1c2VySWQiOjEsInJvbGUiOiJ1c2VyIn0= 
        → {"userId":1,"role":"user"}
3. 篡改：{"userId":1,"role":"admin"}
4. 编码：eyJ1c2VySWQiOjEsInJvbGUiOiJhZG1pbiJ9
5. 提交篡改后的数据
```

**为什么危险**：
Base64 只是编码，不是加密，没有防篡改能力。

---

### 4. XXE 外部实体攻击

**什么是 XXE**：
XML External Entity（XML 外部实体）注入，利用 XML 解析器处理外部实体的功能。

**攻击示例**：
```xml
<?xml version="1.0"?>
<!DOCTYPE foo [
  <!ENTITY xxe SYSTEM "file:///etc/passwd">
]>
<user>
  <name>&xxe;</name>
</user>
```

**可以做什么**：
- 读取本地文件
- SSRF（访问内网服务）
- 拒绝服务（Billion Laughs 攻击）

**Billion Laughs**：
```xml
<!DOCTYPE lolz [
  <!ENTITY lol "lol">
  <!ENTITY lol2 "&lol;&lol;&lol;&lol;&lol;&lol;&lol;&lol;&lol;&lol;">
  <!ENTITY lol3 "&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;">
  ...
]>
```

---

### 5. 语言特定的反序列化漏洞

**Java 反序列化**：
```java
// 危险：直接反序列化不可信数据
ObjectInputStream ois = new ObjectInputStream(untrustedData);
Object obj = ois.readObject();  // 可能触发恶意代码
```
著名工具：ysoserial

**PHP 反序列化**：
```php
// 危险：反序列化用户输入
$obj = unserialize($_GET['data']);
// 可能触发 __wakeup(), __destruct() 等魔术方法
```

**Python Pickle**：
```python
import pickle
# 危险：反序列化不可信数据
obj = pickle.loads(untrusted_data)
# Pickle 可以执行任意代码
```

---

### 6. 防御措施

**JSON 安全处理**：
```python
# 白名单验证
ALLOWED_FIELDS = ['username', 'email']
def safe_update(data):
    return {k: v for k, v in data.items() if k in ALLOWED_FIELDS}
```

**禁用 XML 外部实体**：
```java
// Java
DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);

// Python
from defusedxml import ElementTree
```

**避免危险的反序列化**：
```
1. 优先使用 JSON 而非二进制序列化
2. 使用签名验证数据完整性
3. 使用白名单限制可反序列化的类
4. 保持依赖库更新
```

---

### 7. 结语

反序列化漏洞的可怕之处在于它能直接导致 RCE。攻击者不需要找注入点，只需要找到一个反序列化入口，构造恶意的序列化数据即可。

记住：**永远不要反序列化不可信的数据，如果必须，请使用安全的替代方案。**

---

**[准备好了吗？去"解码"那些危险的数据吧]**
