# 🔍 Lab Manual: Information Disclosure Vulnerabilities

### 0. Preface
Information disclosure is the "first step" in penetration testing and the most easily overlooked vulnerability. It won't directly let you control the server, but it tells you HOW to control the server.

*   **Information Disclosure**: Systems unintentionally expose sensitive information that shouldn't be accessible externally.
*   **Core Concept**: Attackers don't need to guess because you've already written the answers on the wall.

This chapter's goal: **Understand why a single HTML comment could be the starting point of an attack.**

---

### 1. HTML Comment Disclosure

**The Problem**: Comments left by developers in HTML are ignored by browsers but seen by attackers.

**Dangerous Examples**:
```html
<!-- TODO: Delete this test account admin/test123 -->
<!-- DEBUG: API key = sk-xxxxx -->
<!-- Internal address: http://192.168.1.100:8080 -->
<!-- Old endpoint: /api/v1/users (deprecated but not removed) -->
```

**How to Find**:
```bash
curl -s https://target.com | grep -i "<!--"
```

**Lesson**:
HTML comments are not "private notes"—everyone can see them.

---

### 2. API Over-Exposure

**The Problem**: APIs return more data than the frontend needs.

**Dangerous Example**:
```json
// Frontend only needs username and avatar
// But API returns:
{
    "username": "alice",
    "avatar": "...",
    "email": "alice@secret.com",
    "phone": "138xxxxx",
    "password_hash": "5f4dcc3b5aa765d61d8327deb882cf99",
    "internal_id": "emp_12345",
    "department": "Finance"
}
```

**Another Example - Enumeration Vulnerability**:
```
GET /api/user/1 → 200 {"username": "admin"}
GET /api/user/2 → 200 {"username": "alice"}
GET /api/user/999 → 404 "User not found"
# Attacker can enumerate all users
```

---

### 3. Backup File Disclosure

**Commonly Leaked Files**:
```
/.git/                    # Source code!
/.svn/                    # Source code!
/.env                     # DB passwords, API keys
/config.php.bak           # Config file backup
/database.sql             # Database export
/backup.zip               # Website backup
/www.zip                  # Website package
/.DS_Store                # Mac directory structure
/WEB-INF/web.xml          # Java configuration
/robots.txt               # May reveal sensitive paths
/sitemap.xml              # Website structure
```

**Git Leak Exploitation**:
```bash
# If /.git/ is accessible
wget -r https://target.com/.git/
cd target.com
git checkout .
# Now you have complete source code
```

---

### 4. Error Message Disclosure

**The Problem**: Detailed error messages expose tech stack and internal structure.

**Disclosure Example**:
```
Stack Trace:
    at com.myapp.controller.UserController.login(UserController.java:42)
    at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
    
Database Error:
    Connection failed: mysql://root:password123@192.168.1.50:3306/prod_db
```

**Information Gathered from Errors**:
- Programming language and framework
- Database type and location
- Internal IP addresses
- File path structure
- Potential usernames/passwords

---

### 5. HTTP Response Header Disclosure

**Dangerous Response Headers**:
```http
Server: Apache/2.4.41 (Ubuntu)
X-Powered-By: PHP/7.4.3
X-AspNet-Version: 4.0.30319
```

**Why It's Dangerous**:
Attackers can look up known vulnerabilities based on version numbers.

**Fix**:
```nginx
# Nginx
server_tokens off;

# Apache
ServerTokens Prod
ServerSignature Off
```

---

### 6. Defense Measures

**Code Level**:
```python
# Only include necessary fields when returning data
def get_user_profile(user_id):
    user = User.query.get(user_id)
    return {
        "username": user.username,
        "avatar": user.avatar
        # Don't return sensitive fields
    }
```

**Deployment Level**:
```
□ Remove all debug info and comments before going live
□ Block access to .git/.svn/.env and other sensitive directories
□ Configure custom error pages
□ Remove version info from response headers
□ Regularly scan for sensitive file exposure
```

**Minimum Information Principle**:
Only return the minimum information needed to complete the function—not one extra field.

---

### 7. Conclusion

Information disclosure seems "harmless," but it's the first link in the attack chain. Attackers piece together these fragments of information to construct complete attack paths.

Remember: **Every piece of information you leak could become ammunition for attackers.**

---

**[Ready? Go "peek" at what the target has leaked]**
