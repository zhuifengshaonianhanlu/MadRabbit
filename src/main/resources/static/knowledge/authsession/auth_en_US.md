# 🚪 Lab Manual: Authentication & Session Security (Who gave you the illusion of entry?)

### 0. Preface
In the world of Web, the server is like a security guard suffering from "severe short-term memory loss." Every single minute, he forgets who you are.

*   **Authentication**: The process of proving "who I am" to the guard (usually via username and password).
*   **Session**: A temporary "entry pass" (Cookie or Token) issued by the guard after he checks your ID.

The goal of this chapter is simple: **Either trick the guard into misidentifying someone, or forge that entry pass yourself.**

---

### 1. Brute Force: Constant dripping wears away the stone
**The Concept**:
If a login interface has no captcha, no rate limiting, and no IP lockout, it’s not a "Login Portal"—it’s a "Password Validation API." As long as a hacker's dictionary is large enough, your server is basically their backyard.

**Fun Fact**:
The strength of most users' passwords usually depends on their pet's name or the sequence `123456`.

**Pro-Tip**:
Never trust frontend validation. Limiting login frequency in JavaScript is like hanging a sign on your front door that says, "Please don't knock more than three times." It’s cute, but it doesn't stop anyone.

---

### 2. User Enumeration: Guess who's home?
**The Concept**:
When a login fails, if your backend returns:
*   `404 - User does not exist`
*   `401 - Incorrect password`

Congratulations, you are providing a free "Valid Username Directory Service" to hackers for the entire site.

**Dry Humor**:
A good programmer should learn how to lie. No matter who knocks, always reply with a uniform: "Invalid username or password." This "ambiguous aesthetic" not only protects privacy but also drives hackers to smash their keyboards.

---

### 3. Session Fixation: Murder with a Borrowed Knife
**The Concept**:
The server is too lazy. It issues a `JSESSIONID` before the user even logs in, and—miraculously—keeps using the exact same one after they’ve authenticated.

**Attack Script**:
1. I grab a valid ID from the site (e.g., `ABC`).
2. I send `http://target.com/?jsessionid=ABC` to the victim (or trick their browser into setting this cookie).
3. The victim logs in.
4. Since the ID didn't change, the `ABC` in my hand is now automatically upgraded to the victim's "authorized pass."

**Core Philosophy**:
If you don't change the pass, I'll assume you never left.

---

### 4. Weak Session Identifiers: Base64 is NOT Encryption
**The Concept**:
Some developers find `JSESSIONID` too hard to remember, so they invent their own.
For example: `Cookie: user_id=admin`.
Or: `Cookie: user_id=YWRtaW4=` (This is just "admin" in Base64. It’s not encryption; it’s just a translation).

**The Irony**:
If you can become an admin just by changing a number or a string in your Cookie, your security architecture is about as effective as a "Keep Out" sign written in crayon.

---

### 5. JWT: The "Algorithm of Shame"
**The Concept**:
JWT (JSON Web Token) is trendy, but it gives humans more opportunities to mess up.
*   **None Algorithm**: A hacker changes the `alg` field in the Header to `none`. If the backend doesn't check for this, it might blindly trust the unverified Token.
*   **Weak Secrets**: If your signing key is `123456`, a hacker can brute-force it in 0.01 seconds and then promote themselves to "Super Admin."

**Warning**:
A JWT is like a letter. Even if it's sealed, if the envelope is transparent and the seal was made with saliva, anyone can rewrite the contents.

---

### 6. Logic Flaws: The "Backdoor" of Forgotten Passwords
**The Concept**:
Some systems ask, "Who are you?" during a password reset.
When you answer "admin," it replies: "Okay, a code has been sent to your email. By the way, here is your password reset link: `.../reset?user=admin`."

**Operational Guide**:
In these levels, keep your eyes glued to every HTTP response packet. Sometimes, the password or the reset link is hidden inside that one extra sentence the server "accidentally" blurted out.

---

### 7. Conclusion
The core of authentication security is just one sentence: **Never trust a client-side claim of "who I am" unless you have verified it with the most rigorous backend logic.**

Now, head to the lab and try it out. If you can't get in, that's normal. If you do get in, it means the "security guard" deserves to be fired.

---

**[Are you ready? Let the hacking begin]**
