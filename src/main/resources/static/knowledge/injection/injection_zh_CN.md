# 💉 实验手册：注入攻击

### 0. 序言
在计算机安全领域，"注入"是一种经典而危险的攻击手法。当应用程序不加验证地将用户输入拼接到命令或查询中时，攻击者就有机会让系统执行本不该执行的操作。

*   **注入攻击（Injection）**：通过向程序输入中插入恶意代码，改变程序的执行逻辑或获取未授权的数据。
*   **常见类型**：SQL注入、命令注入、LDAP注入、XPath注入等。

本章练习的目标：**理解注入攻击的原理，掌握基本的利用技术。**

---

### 1. SQL注入：数据库的后门

**原理简介**：
当应用程序将用户输入直接拼接到SQL查询语句中，而没有进行适当的过滤或参数化处理时，攻击者可以通过构造特殊的输入来操纵SQL语句，从而绕过认证、窃取数据或破坏数据库。

**经典场景**：
```sql
-- 正常登录查询
SELECT * FROM users WHERE username='admin' AND password='secret'

-- 攻击者输入：admin' OR '1'='1
SELECT * FROM users WHERE username='admin' OR '1'='1' AND password=''
-- 由于 '1'='1' 永真，查询返回所有用户！
```

**攻击类型**：
- **认证绕过**：使用 `' OR '1'='1` 绕过登录
- **联合查询注入**：使用 `UNION SELECT` 获取其他表的数据
- **盲注**：通过布尔条件或时间延迟推断数据
- **堆叠查询**：使用 `;` 执行多条SQL语句

**冷知识**：
SQL注入漏洞在1998年被首次公开讨论，至今仍是OWASP Top 10的常客。很多大规模数据泄露事件的罪魁祸首就是SQL注入。

---

### 2. 命令注入：系统的钥匙

**原理简介**：
当应用程序将用户输入直接传递给系统命令执行（如shell命令）时，攻击者可以通过特殊字符来注入额外的命令，从而在服务器上执行任意操作。

**危险场景**：
```bash
# 应用程序代码（伪代码）
system("ping -c 3 " + userInput)

# 用户输入：127.0.0.1; cat /etc/passwd
# 实际执行：ping -c 3 127.0.0.1; cat /etc/passwd
# 结果：先执行ping，然后显示系统密码文件！
```

**命令分隔符**：
| 符号 | 作用 | 示例 |
|------|------|------|
| `;` | 命令分隔符 | `cmd1; cmd2` |
| `\|` | 管道符 | `cmd1 \| cmd2` |
| `&&` | 前命令成功则执行 | `cmd1 && cmd2` |
| `\|\|` | 前命令失败则执行 | `cmd1 \|\| cmd2` |
| `` `cmd` `` | 命令替换 | `echo `whoami`` |
| `$(cmd)` | 命令替换 | `echo $(whoami)` |

**核心思想**：
你以为程序只是ping一下，其实它可能正在帮你"查看"服务器的秘密。

---

### 3. 常见注入 Payload

**SQL注入 - 认证绕过**：
```sql
' OR '1'='1
' OR '1'='1' --
admin' --
' OR 1=1 #
```

**SQL注入 - 联合查询**：
```sql
' UNION SELECT 1,2,3--
' UNION SELECT username,password FROM users--
' UNION SELECT null,table_name FROM information_schema.tables--
```

**命令注入**：
```bash
127.0.0.1; ls -la
127.0.0.1 | cat /etc/passwd
127.0.0.1 && whoami
`id`
$(cat /etc/passwd)
```

---

### 4. 注入攻击的危害

**SQL注入的后果**：
- 🔓 **认证绕过**：不需要密码就能登录任意账户
- 📊 **数据泄露**：获取数据库中的敏感信息（用户数据、信用卡号等）
- 🗑️ **数据破坏**：删除或篡改数据库内容
- 🔑 **权限提升**：获取管理员权限
- 🚪 **后门植入**：在数据库中创建后门账户

**命令注入的后果**：
- 💻 **任意命令执行**：以Web服务器权限执行任何系统命令
- 📁 **文件读取/写入**：读取敏感配置文件，写入WebShell
- 🔙 **反弹Shell**：获取服务器的远程访问权限
- 🌐 **内网渗透**：以服务器为跳板攻击内网

---

### 5. 防御措施速览

**SQL注入防御**：

**1. 参数化查询（首选）**：
```java
// 错误示例
String sql = "SELECT * FROM users WHERE username='" + username + "'";

// 正确示例 - 使用PreparedStatement
PreparedStatement stmt = conn.prepareStatement("SELECT * FROM users WHERE username=?");
stmt.setString(1, username);
```

**2. 输入验证**：
```java
// 白名单验证
if (!username.matches("^[a-zA-Z0-9_]+$")) {
    throw new IllegalArgumentException("Invalid username");
}
```

**3. 最小权限原则**：
- 数据库账户只授予必要的权限
- 不同功能使用不同的数据库账户

**命令注入防御**：

**1. 避免直接调用系统命令**：
```java
// 尽量使用语言内置的库函数
InetAddress.getByName(host).isReachable(5000);
```

**2. 输入验证和转义**：
```java
// 只允许合法的IP地址格式
if (!host.matches("^[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}$")) {
    throw new IllegalArgumentException("Invalid IP address");
}
```

**3. 使用参数数组而非字符串拼接**：
```java
// 使用ProcessBuilder而非Runtime.exec(String)
ProcessBuilder pb = new ProcessBuilder("ping", "-c", "3", host);
```

---

### 6. 结语

注入攻击的本质是"混淆代码和数据的边界"——当用户输入被当作代码的一部分执行时，安全就被打破了。

记住：**永远不要信任用户输入，永远使用参数化/预编译的方式处理用户数据。**

现在，去靶场试试你的技能吧。如果你成功绕过了登录验证或执行了系统命令，那说明你已经理解了注入攻击的精髓。但更重要的是，学会如何防御这些攻击。

---

**[准备好了吗？开始注入吧]**
