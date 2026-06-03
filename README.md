**[中文](README_ZH.md)** | English

# MadRabbit: A Web Security Vulnerability Lab Platform




## 1. Introduction

MadRabbit is a vulnerability lab platform designed for web security learners and researchers. It covers 13 common web vulnerability categories, each containing multiple challenge levels with different business scenarios. Through studying and practicing these vulnerabilities, you can become a proficient web security engineer.

### 1.1 A Note

In an era where AI can discover 10,000+ vulnerabilities in a month, is it still useful to learn the underlying principles of vulnerabilities?

The answer is simple: when interviewing for a security engineer position, those who deeply understand vulnerability principles will be chosen — not those who only know how to use tools.

**Who you are determines how you work. Don't be a script kid, nor an AI kid.**

### 1.2 Vulnerability Categories

The platform covers the following 13 web security vulnerability categories with 50+ challenge levels:

| Category | Description |
|----------|-------------|
| Authentication & Session | Brute force, JWT security, password reset flaws |
| SQL Injection | Login bypass, UNION injection, MyBatis injection, blind injection |
| XXE | File read, OOB exfiltration, Content-Type switching |
| XSS | Reflected, Stored, DOM-based XSS |
| CSRF | GET/POST-based CSRF, token bypass |
| SSRF | Basic SSRF, protocol smuggling, bypass techniques, redirect chains |
| RCE | Command injection, code evaluation, bypass techniques |
| Access Control | Horizontal/vertical privilege escalation, IDOR |
| File Operations | File upload, path traversal, file inclusion |
| Deserialization | Java native deserialization, Fastjson RCE, Log4Shell |
| Security Configuration | Actuator exposure, Swagger leakage |
| Sensitive Information Leakage | Error-based leaks, Git exposure, hardcoded secrets |
| Business Logic | Coupon abuse, price tampering, process skipping |

---

## 2. Prerequisites

Before deploying, ensure the following software is installed:

| Dependency | Minimum Version | Purpose | Verify |
|------------|----------------|---------|--------|
| JDK | 17 | Runtime | `java -version` |
| Maven | 3.6+ | Build | `mvn -version` |
| MySQL | 8.0+ | Database | `mysql --version` |
| IDE | Any | Source debugging | Your preferred IDE |

---

## 3. Deployment from Source

> **Tip**: To deeply understand vulnerability principles from the source code level, we strongly recommend deploying from source.

### 3.1 Clone the Repository

```bash
git clone <repository-url> madX
cd madX
```

### 3.2 Start MySQL and Create the Database

Ensure the MySQL service is running:

```bash
# macOS (Homebrew)
brew services start mysql

# Linux (systemd)
sudo systemctl start mysql

# Verify connection
mysql -u root -p -e "SELECT VERSION();"
```

Run the one-click initialization script to create the database, all tables, and seed data:

```bash
mysql -u root -p < sql/install_init.sql
```

The script will automatically:
- Create the `madrabbit` database (UTF-8MB4 encoding)
- Create 6 business tables (users, flags, challenge_users, logs_access, logs_error, merchant_orders)
- Insert all initial data (users, challenge flags, injection data, authorization order data, etc.)

> **Tip**: The script uses `INSERT IGNORE` syntax, so it can be safely run multiple times without primary key conflicts.

Verify the initialization:

```bash
mysql -u root -p madrabbit -e "SHOW TABLES; SELECT COUNT(*) AS user_count FROM users; SELECT COUNT(*) AS flag_count FROM flags;"
```

Expected output: 6 tables, 6 users, 50+ flag records.

### 3.3 Configure Database Connection

Edit `src/main/resources/application.yml` and update the database connection:

```yaml
spring:
  datasource:
    url: jdbc:mysql://127.0.0.1:3306/madrabbit?useSSL=false&serverTimezone=UTC&characterEncoding=utf8
    username: YOUR_DB_USERNAME     # Your MySQL username
    password: YOUR_DB_PASSWORD     # Your MySQL password
```

### 3.4 Build the Project

> **Tip**: We recommend opening the project directly in your IDE for building and running. Below are manual Maven commands:

```bash
mvn clean package -DskipTests
```

The first build will download dependencies and may take a while. On success, `madrabbit-1.0.0.jar` will be generated in the `target/` directory.

> **Troubleshooting**: If dependency downloads are slow, configure a Maven mirror (e.g., Alibaba Cloud) in `~/.m2/settings.xml`.

### 3.5 Start the Application

**Option 1: Run with Maven (recommended for development)**

```bash
mvn spring-boot:run
```

**Option 2: Run the JAR (recommended for deployment)**

```bash
java -jar target/madrabbit-1.0.0.jar
```

> **Note**: Deserialization challenges (Log4Shell/Java Deserialization) require JVM module access. The `pom.xml` already configures the necessary `--add-opens` flags via `spring-boot-maven-plugin`. These are applied automatically when using `mvn spring-boot:run`. For `java -jar`, add them manually:
>
> ```bash
> java --add-opens java.naming/javax.naming=ALL-UNNAMED \
>      --add-opens java.base/java.lang=ALL-UNNAMED \
>      --add-opens java.base/java.lang.reflect=ALL-UNNAMED \
>      -jar target/madrabbit-1.0.0.jar
> ```

### 3.6 Verify Deployment

After successful startup, you should see:

```
Started MadrabbitApplication in X.XXX seconds
```

Visit the following URL to confirm the service is running:

| URL | Description |
|-----|-------------|
| http://localhost:8080/login.html | Login page |

---

## 4. Default Test Accounts

| Username | Password | Role |
|----------|----------|------|
| admin | 123456 | ADMIN |
| jack | 123456 | LEARNER |
| lucy | 123456 | LEARNER |
| tom | 123456 | LEARNER |

Log in to browse the vulnerability categories and enter challenge levels.

---

## 5. Tech Stack

- **Backend**: Java 17 + Spring Boot 2.7 + MyBatis
- **Database**: MySQL 8.0+
- **Frontend**: HTML + CSS + JavaScript (vanilla)
- **Build Tool**: Maven

---

## 6. Security Warning

> **WARNING**: This project contains **intentionally designed security vulnerabilities** for educational purposes only.
> **DO NOT** deploy it in production environments or expose it to the public internet.
> Any security risks or consequences arising from misuse are the sole responsibility of the user.

---

## 7. License

This project is licensed under a custom license. See the [LICENSE](LICENSE) file for details.

- Free for personal learning and educational training purposes
- **Prohibited** without prior written authorization: modification, commercial use, derivative works, or redistribution

---

## 8. Star History

If you find this project helpful, please give it a star!
