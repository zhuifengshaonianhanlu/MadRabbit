# 🎭 实验手册：CSRF 跨站请求伪造

### 0. 序言
如果说 XSS 是让浏览器执行恶意代码，那 CSRF 就更狡猾——它让浏览器在你不知情的情况下，用你的身份做坏事。

*   **CSRF（Cross-Site Request Forgery）**：一种利用用户已登录状态，诱骗浏览器发送恶意请求的攻击。
*   **核心思想**：你以为你在刷微博，其实你在帮黑客转账。

本章的目标：**让你理解为什么"点一下链接就被盗号"是真实存在的。**

---

### 1. CSRF 的工作原理

**攻击三要素**：
1. 用户已登录目标网站（浏览器存有有效 Cookie）
2. 用户访问了攻击者的恶意页面
3. 恶意页面向目标网站发送请求

**核心原理**：
```
浏览器的同源策略：我不让你读别人的响应
CSRF 漏洞：但我允许你发请求...带着 Cookie
```

**冷知识**：
浏览器会自动携带对应域名的 Cookie，这本是为了方便用户，却成了 CSRF 的帮凶。

---

### 2. GET 型 CSRF：一张图片的陷阱

**原理**：
利用 GET 请求可以通过 `<img>` 标签发起的特性。

**攻击示例**：
```html
<!-- 看起来只是一张图片 -->
<img src="http://bank.com/transfer?to=hacker&amount=10000" />

<!-- 用户访问包含此代码的页面时，浏览器会自动发送请求 -->
<!-- 如果用户恰好登录了 bank.com，转账就完成了 -->
```

**讽刺点**：
用户以为在看猫片，实际上在给黑客打钱。

---

### 3. POST 型 CSRF：隐藏的表单

**原理**：
虽然 POST 请求不能通过图片发起，但可以通过自动提交的表单实现。

**攻击示例**：
```html
<form id="evil-form" action="http://bank.com/transfer" method="POST">
    <input type="hidden" name="to" value="hacker" />
    <input type="hidden" name="amount" value="10000" />
</form>
<script>document.getElementById('evil-form').submit();</script>
```

**升级版**：用 iframe 隐藏提交过程，用户完全无感知。

---

### 4. 无效的 CSRF Token

**常见误区**：加了 CSRF Token 就安全了？不一定。

**漏洞场景**：
```java
// 服务端只验证 Token 是否存在，不验证 Token 归属
if (validTokens.containsKey(csrf_token)) {
    // 通过验证 -- 但这个 Token 可能是别的用户的！
    processRequest();
}
```

**攻击方法**：
```html
<!-- 攻击者用自己的账号登录，获取自己的 CSRF Token -->
<!-- 然后在恶意页面中使用自己的 Token 冒充受害者 -->
<img src="http://target.com/change-email?email=hacker@evil.com&csrf_token=ATTACKER_OWN_TOKEN" />
```

**关键**：
Token 验证了"存在性"，却忽略了"归属权"。别人的门禁卡也能刷开你的门。

---

### 5. 防御措施详解

**CSRF Token**：
```html
<!-- 服务器生成随机 Token，嵌入表单 -->
<input type="hidden" name="csrf_token" value="a7x9k2m...">
```
攻击者无法获取这个 Token，因为同源策略阻止他读取目标页面的内容。

**SameSite Cookie**：
```http
Set-Cookie: session=abc123; SameSite=Strict
```
- `Strict`：完全禁止跨站携带
- `Lax`：允许顶级导航的 GET 请求携带
- `None`：允许跨站携带（需配合 Secure）

**双重验证**：
关键操作要求输入密码或验证码，即使 CSRF 成功也无法完成攻击。

---

### 6. 结语

CSRF 的可怕之处在于它的隐蔽性——用户什么都没做，只是打开了一个网页，账户就被操作了。

记住：**永远不要信任来自浏览器的请求，即使它带着有效的 Cookie。**

---

**[准备好了吗？去体验被"钓"的感觉吧]**
