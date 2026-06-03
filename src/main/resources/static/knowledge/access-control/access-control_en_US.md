# 🚪 Lab Manual: Access Control Vulnerabilities

### 0. Preface
Logged in means you can do anything? Naive. Access control is the last line of defense determining "who can do what," and it's often overlooked by developers.

*   **Access Control**: The mechanism that determines what resources authenticated users can access and what operations they can perform.
*   **Core Concept**: You are you, but you can't become someone else; you're a user, but you're not an admin.

This chapter's goal: **Understand why "changing one ID" lets you see someone else's data.**

---

### 1. Horizontal vs Vertical Privilege Escalation

**Horizontal Privilege Escalation**:
```
User A accesses User B's resources
Example: /user/profile?id=1001 → /user/profile?id=1002
Privilege escalation between same-level users
```

**Vertical Privilege Escalation**:
```
Regular user accesses admin functionality
Example: Regular user accessing /admin/users
Low-privilege user gains high-privilege functions
```

**Memory Trick**:
- Horizontal: Visiting neighbors on the same floor
- Vertical: Regular person sneaking into VIP area

---

### 2. IDOR - Insecure Direct Object Reference

**What is IDOR**:
Insecure Direct Object Reference—accessing unauthorized resources by modifying identifiers in requests.

**Classic Example**:
```
# View your own order
GET /api/order/12345

# Modify ID to view others' orders
GET /api/order/12346
GET /api/order/12347

# If server doesn't verify ownership, attack succeeds!
```

**Common IDOR Scenarios**:
- Order details, user profiles, private messages
- File downloads: `/download?file=report_1001.pdf`
- Data exports: `/export?userId=1001`

---

### 3. Common Access Control Flaws

**Frontend-only Control**:
```javascript
// Frontend hides admin button
if (!user.isAdmin) {
    adminButton.style.display = 'none';
}
// But backend API has no permission verification!
```

**Coarse-grained Path-based Control**:
```
/admin/* → Requires admin privileges
/api/user/* → Requires login

# But what about /api/admin/users?
```

**Predictable Parameters**:
```
User IDs use incremental numbers: 1001, 1002, 1003...
Order numbers use timestamps: 20240101001, 20240101002...
```

---

### 4. RBAC vs ABAC

**RBAC (Role-Based Access Control)**:
```
User → Role → Permissions
Example: John → Manager → [View Reports, Approve Leave]
```

**ABAC (Attribute-Based Access Control)**:
```
Dynamic decisions based on user attributes, resource attributes, environmental conditions
Example: Department=Sales AND Level>=5 AND Time=WorkHours → Allow customer data access
```

**Recommendation**:
- Simple scenarios: RBAC is sufficient
- Complex scenarios: ABAC is more flexible

---

### 5. Real Vulnerability Cases

**Case 1: Modifying user ID to view others' info**
```
POST /api/user/profile
{"userId": 1001}  →  {"userId": 1002}
```

**Case 2: Unauthorized password change for others**
```
POST /api/user/changePassword
{"userId": 1001, "newPassword": "hacked"}
```

**Case 3: Regular user accessing admin functions**
```
# Frontend hid the entrance, but direct API access
GET /api/admin/getAllUsers
```

---

### 6. Defense Measures

**Server-side Mandatory Verification**:
```python
def get_order(order_id, current_user):
    order = Order.query.get(order_id)
    if order.user_id != current_user.id:
        raise PermissionError("You can only view your own orders")
    return order
```

**Use Unpredictable Identifiers**:
```python
import uuid
order_id = str(uuid.uuid4())  # a1b2c3d4-e5f6-7890-...
```

**Least Privilege Principle**:
```
Deny all access by default
Grant only minimum permissions needed for the task
Regularly review permission configurations
```

**Unified Permission Check Layer**:
```java
@PreAuthorize("hasRole('ADMIN') or #userId == authentication.principal.id")
public User getUser(Long userId) { ... }
```

---

### 7. Conclusion

What makes access control vulnerabilities terrifying is their stealth—functionality looks completely normal, except "you can see things you shouldn't."

Remember: **Always verify on the server side whether the current user has permission to access the requested resource.**

---

**[Ready? Go see other people's secrets (within authorized scope)]**
