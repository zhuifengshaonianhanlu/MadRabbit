# ⚙️ Lab Manual: Security Misconfiguration

### 0. Preface
The strongest lock can't stop an open door. Security misconfiguration IS that open door—it's not that the code is bad, it's that nothing was configured properly.

*   **Security Misconfiguration**: Security issues caused by incorrect system, application, or service configurations.
*   **Core Concept**: Your code might be secure, but your configuration might be "running naked."

This chapter's goal: **Understand why "using default passwords" is one of the most common intrusion methods.**

---

### 1. Default Credentials: The Easiest Entry Point

**Common Default Accounts**:
```
admin:admin
admin:123456
root:root
administrator:password
tomcat:tomcat
test:test
```

**High-Risk Service Default Credentials**:
```
MySQL: root:(empty password)
Redis: (no password)
MongoDB: (no authentication)
Elasticsearch: (no authentication)
Jenkins: admin:admin
```

**Fun Fact**:
Many enterprise breaches started with a forgotten default password.

---

### 2. Error Message Disclosure

**The Problem**: Detailed error messages reveal internal system structure.

**Dangerous Example**:
```
java.sql.SQLException: Column 'password' not found
    at com.mysql.jdbc.SQLError.createSQLException(SQLError.java:1073)
    at com.myapp.dao.UserDAO.findByUsername(UserDAO.java:42)
```

What's leaked?
- Database type (MySQL)
- Table structure (has password column)
- Code structure (UserDAO.java)
- Line number (42)

**Fix**:
```
Production environment:
- Display generic error: "System error, please try again later"
- Log detailed errors, don't show to users
- Disable debug mode
```

---

### 3. HTTP Security Headers

**Content-Security-Policy (CSP)**:
```http
Content-Security-Policy: default-src 'self'; script-src 'self'
```
Restricts resource loading sources, prevents XSS.

**X-Frame-Options**:
```http
X-Frame-Options: DENY
```
Prevents page from being embedded in iframe, prevents clickjacking.

**Strict-Transport-Security (HSTS)**:
```http
Strict-Transport-Security: max-age=31536000; includeSubDomains
```
Forces HTTPS usage.

**X-Content-Type-Options**:
```http
X-Content-Type-Options: nosniff
```
Prevents MIME type sniffing.

**X-XSS-Protection** (deprecated but still useful):
```http
X-XSS-Protection: 1; mode=block
```

---

### 4. Insecure HTTP Methods

**Dangerous Methods**:
```http
PUT /shell.jsp HTTP/1.1
# Directly upload files to server

DELETE /important-file HTTP/1.1
# Delete server files

TRACE /test HTTP/1.1
# May leak cookies (XST attack)
```

**Detection Method**:
```http
OPTIONS / HTTP/1.1
Host: target.com

# Response
Allow: GET, HEAD, POST, PUT, DELETE, OPTIONS
```

**Fix**:
Only allow necessary HTTP methods, disable PUT, DELETE, TRACE, etc.

---

### 5. Common Configuration Issues Checklist

**Directory Listing Enabled**:
```
Visiting /images/ shows all file listings
```

**Sensitive File Exposure**:
```
/.git/              # Git repository
/.svn/              # SVN repository
/.env               # Environment variables
/web.config         # IIS configuration
/phpinfo.php        # PHP info
/server-status      # Apache status
/.htaccess          # Apache configuration
/backup.sql         # Database backup
```

**Debug Features Enabled**:
```
Spring Boot Actuator: /actuator/env
Swagger/OpenAPI: /v3/api-docs, /swagger-ui/index.html
Django Debug: Detailed error pages
```

---

### 6. Defense Measures

**Security Configuration Checklist**:
```
□ Change all default passwords
□ Disable unnecessary services and ports
□ Turn off directory listing
□ Configure appropriate HTTP security headers
□ Disable debug mode in production
□ Regularly scan for sensitive file exposure
□ Configure accounts with least privilege
□ Enable access logging and monitoring
```

**Automated Scanning**:
```bash
# Use nmap to scan open ports
nmap -sV target.com

# Use nikto to scan web configuration issues
nikto -h http://target.com
```

---

### 7. Conclusion

Security misconfiguration is the easiest vulnerability to make and exploit. It doesn't require advanced skills—just an attacker trying default passwords or visiting `/.git/`.

Remember: **Security is an ongoing process. Going live is just the beginning; configuration hardening is a never-ending journey.**

---

**[Ready? Go check if your configuration is "running naked"]**
