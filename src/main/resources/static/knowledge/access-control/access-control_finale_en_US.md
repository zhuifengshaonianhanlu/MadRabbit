# Access Control Vulnerabilities - Final Summary

Congratulations on completing all levels of the Access Control module! In this module, you've hands-on practiced three classic privilege escalation attack patterns. Let's review and summarize in depth.

---

## Learning Path Review

### Horizontal Escalation — Merchant Order Center

**What you learned:**
- When the backend doesn't verify resource ownership, an order number alone is enough to view another merchant's sensitive information
- Order numbers follow a predictable pattern (date + 5 random digits), making enumeration attacks feasible
- Masked display ≠ security — as long as the decryption API lacks ownership verification, the data is exposed

**Attack essence:**
```
User A at the same privilege level accesses User B's data
Root cause: Server doesn't verify "does this data belong to the current requester?"
```

**Fix:**
```java
// Add ownership verification to every query
SELECT * FROM merchant_orders
WHERE order_no = #{orderNo} AND merchant_id = #{currentUserId}
```

---

### Vertical Escalation — Role Privilege Escalation Chain

**What you learned:**
- Frontend disabled fields are merely a "gentleman's agreement" — not a security control
- If the backend doesn't validate "who is modifying which field", anyone can tamper with any value
- Attacks can form a chain: first escalate privileges (modify role), then exploit the new privileges

**Attack essence:**
```
Regular user → Tamper own role to ADMIN → Gain admin privileges → Execute admin operations
Root cause: PUT /api/users/{id} doesn't verify "is the current user allowed to modify the role field?"
```

**Fix:**
```java
// Option 1: Sensitive field whitelist
public int updateUser(User user, User currentUser) {
    // Non-admins cannot modify the role field
    if (!"ADMIN".equals(currentUser.getRole())) {
        user.setRole(null); // Ignore role modification
    }
    return userRepository.updateUser(user);
}

// Option 2: Dedicated role modification endpoint + permission annotation
@PreAuthorize("hasRole('ADMIN')")
@PutMapping("/users/{id}/role")
public void updateUserRole(...) { ... }
```

---

### Frontend's Secret — Hidden API Discovery & IDOR

**What you learned:**
- Frontend code is completely public — comments, dead code, and debug info can all leak backend endpoints
- Even if the page doesn't provide an action button, the backend API still exists and can be called directly
- Security cannot rely on the assumption that "users don't know this endpoint exists"

**Attack essence:**
```
Audit frontend source → Find commented API endpoint → Call it directly → No ownership check → Unauthorized access
This is a combination of "information disclosure" + "IDOR"
```

**Fix:**
```
1. Remove all debug code and API info from comments in production
2. Use code obfuscation and tree-shaking during frontend builds
3. Every backend endpoint must independently verify permissions and ownership
4. Never assume "users won't know about this endpoint"
```

---

## Three Escalation Patterns Compared

| | Horizontal | Vertical | Frontend's Secret (IDOR) |
|--|-----------|----------|--------------------------|
| **Attack Target** | Same-level user's data | Higher privilege operations | Hidden API + other's data |
| **Root Cause** | Missing ownership check | Missing permission check | API exposure + missing ownership check |
| **Prerequisites** | Know other's resource ID | Know admin API / can modify role | Can access frontend source |
| **Impact** | Data breach | Full system takeover | Data breach |
| **Detection Difficulty** | Medium | Lower | Higher |

---

## Access Control Security Design Principles

### 1. Principle of Least Privilege
```
Each user/role should only access the minimum resources necessary for their work.
Don't grant "might need" permissions — only grant "definitely needs" permissions.
```

### 2. Deny by Default
```
Deny any operation not explicitly authorized.
Whitelist > Blacklist.
```

### 3. Server-side Is the Only Trust Boundary
```
Any frontend restriction (disabled, hidden, JS validation) can be bypassed.
All permission decisions must be made on the server side.
Client-side is merely a UX optimization, not a security control.
```

### 4. Independent Verification at the API Level
```
Every API endpoint should independently verify:
① Is the user authenticated? (Authentication)
② Is the user authorized to perform this action? (Authorization)
③ Does the target resource belong to this user? (Ownership)
```

### 5. Unpredictable Resource Identifiers
| Method | Example | Recommended? |
|--------|---------|--------------|
| Auto-increment | 1, 2, 3 | No (enumerable) |
| UUID v4 | `a1b2c3d4-e5f6-...` | Yes |
| Encrypted ID | `Base64(AES(id))` | Yes |
| Date+Random | `20260501-87231` | No (pattern guessable) |

---

## Security Development Checklist

### During API Development
- [ ] Does this endpoint require authentication? → Add auth check
- [ ] Is this operation role-restricted? → Add role/permission check
- [ ] Does this resource have ownership? → Add ownership check
- [ ] Are there fields in the request that "users shouldn't control"? → Use whitelist filtering

### During Code Review
- [ ] Search all UPDATE/DELETE statements — confirm WHERE clause includes user identifier
- [ ] Check role modification / permission change APIs for additional validation
- [ ] Verify frontend code doesn't contain leftover backend API information

### During Testing
- [ ] Replay high-privilege requests with a low-privilege account (Burp Suite / AuthMatrix)
- [ ] Access User B's resources with User A's token
- [ ] Review frontend JS for unused API endpoints

---

## Recommended Tools

**Burp Suite — Authorize Plugin**
```
1. Configure two accounts with different privileges
2. Operate normally with the high-privilege account, recording all requests
3. The plugin automatically replays each request with the low-privilege token
4. Compare responses: if low-privilege also succeeds, access control is broken
```

**Manual Testing Approach**
```
1. Log in as regular user, collect your resource IDs
2. Log in as another regular user, try accessing the first user's resources
3. Modify role/permission/is_admin fields in requests
4. View frontend JS source, search for /api/ or fetch( keywords
```

---

## Core Takeaway

> **Authentication answers "Who are you?"**
> **Authorization answers "What can you do?"**
>
> Successful login ≠ can do anything. Every endpoint, every data record, every operation needs independent permission verification.

The three attacks you experienced in this module happen in the real world every day. In the OWASP Top 10, "Broken Access Control" consistently ranks #1.

Understanding attack principles is for writing more secure code.

---

**[Complete]**
