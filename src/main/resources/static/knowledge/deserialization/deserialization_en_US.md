# 🔓 Lab Manual: Deserialization Vulnerabilities

### 0. Preface
Serialization is for convenient data transfer; deserialization "brings back" the object. But if the deserialized data comes from an attacker, you might be "resurrecting" a Trojan horse.

*   **Deserialization Vulnerabilities**: When an application restores untrusted serialized data into objects, it may lead to code execution or other security issues.
*   **Core Concept**: You think you're restoring data, but you're actually executing code.

This chapter's goal: **Understand why "a Base64 string" can become remote code execution.**

---

### 1. What is Serialization/Deserialization

**Serialization**: Converting objects into storable/transferable format
```
Object → JSON/XML/Binary → Storage/Transfer
```

**Deserialization**: Restoring stored format back to object
```
JSON/XML/Binary → Object → Usage
```

**Common Formats**:
- JSON: `{"name": "Alice", "age": 25}`
- XML: `<user><name>Alice</name></user>`
- Java Serialization: Binary format, starts with `AC ED 00 05`
- PHP Serialization: `O:4:"User":1:{s:4:"name";s:5:"Alice";}`
- Python Pickle: Binary format

---

### 2. JSON Injection and Object Tampering

**Scenario**: Application trusts JSON data from client.

**Attack Example**:
```json
// Normal request
{"userId": 123, "role": "user"}

// Tampered
{"userId": 123, "role": "admin", "isAdmin": true}
```

**More Dangerous - Prototype Pollution (JavaScript)**:
```json
{"__proto__": {"isAdmin": true}}
```
Can pollute prototypes of all objects!

**Defense**:
- Use whitelist to validate fields
- Don't directly merge JSON into objects

---

### 3. Base64 Encoding Tampering

**Common Scenario**:
Application uses Base64-encoded data to pass state or configuration.

**Attack Flow**:
```
1. Intercept request, find Base64-encoded parameter
2. Decode: eyJ1c2VySWQiOjEsInJvbGUiOiJ1c2VyIn0= 
        → {"userId":1,"role":"user"}
3. Tamper: {"userId":1,"role":"admin"}
4. Encode: eyJ1c2VySWQiOjEsInJvbGUiOiJhZG1pbiJ9
5. Submit tampered data
```

**Why It's Dangerous**:
Base64 is just encoding, not encryption—no tamper protection.

---

### 4. XXE External Entity Attacks

**What is XXE**:
XML External Entity injection, exploiting XML parser's external entity handling feature.

**Attack Example**:
```xml
<?xml version="1.0"?>
<!DOCTYPE foo [
  <!ENTITY xxe SYSTEM "file:///etc/passwd">
]>
<user>
  <name>&xxe;</name>
</user>
```

**What It Can Do**:
- Read local files
- SSRF (access internal services)
- Denial of Service (Billion Laughs attack)

**Billion Laughs**:
```xml
<!DOCTYPE lolz [
  <!ENTITY lol "lol">
  <!ENTITY lol2 "&lol;&lol;&lol;&lol;&lol;&lol;&lol;&lol;&lol;&lol;">
  <!ENTITY lol3 "&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;">
  ...
]>
```

---

### 5. Language-Specific Deserialization Vulnerabilities

**Java Deserialization**:
```java
// Dangerous: Directly deserializing untrusted data
ObjectInputStream ois = new ObjectInputStream(untrustedData);
Object obj = ois.readObject();  // May trigger malicious code
```
Famous tool: ysoserial

**PHP Deserialization**:
```php
// Dangerous: Deserializing user input
$obj = unserialize($_GET['data']);
// May trigger __wakeup(), __destruct() magic methods
```

**Python Pickle**:
```python
import pickle
# Dangerous: Deserializing untrusted data
obj = pickle.loads(untrusted_data)
# Pickle can execute arbitrary code
```

---

### 6. Defense Measures

**JSON Safe Handling**:
```python
# Whitelist validation
ALLOWED_FIELDS = ['username', 'email']
def safe_update(data):
    return {k: v for k, v in data.items() if k in ALLOWED_FIELDS}
```

**Disable XML External Entities**:
```java
// Java
DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);

// Python
from defusedxml import ElementTree
```

**Avoid Dangerous Deserialization**:
```
1. Prefer JSON over binary serialization
2. Use signatures to verify data integrity
3. Use whitelist to limit deserializable classes
4. Keep dependencies updated
```

---

### 7. Conclusion

What makes deserialization vulnerabilities terrifying is they can directly lead to RCE. Attackers don't need injection points—just a deserialization entry point and crafted malicious serialized data.

Remember: **Never deserialize untrusted data. If you must, use safe alternatives.**

---

**[Ready? Go "decode" that dangerous data]**
