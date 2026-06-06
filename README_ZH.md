中文 | **[English](README.md)**

# MadRabbit：Web 安全漏洞靶场平台

![Web安全](https://img.shields.io/badge/Web%E5%AE%89%E5%85%A8-%E9%9D%B6%E5%9C%BA-red) ![Java](https://img.shields.io/badge/Java%20%2B%20Spring%20Boot-17%20%2B%202.7-007396) ![版本](https://img.shields.io/badge/%E7%89%88%E6%9C%AC-v1.0-blue) ![GitHub stars](https://img.shields.io/github/stars/zhuifengshaonianhanlu/MadRabbit?style=social) ![GitHub forks](https://img.shields.io/github/forks/zhuifengshaonianhanlu/MadRabbit?style=social) 
![](https://img.shields.io/badge/Status-Developing-green)

## 1. 介绍

MadRabbit 是一个面向 Web 安全学习和研究者的漏洞靶场系统，涵盖了 13 类常见 Web 漏洞，每类漏洞又由多个不同业务场景的关卡组成。通过研究和练习这些漏洞，让你成为一个合格的 、激情燃烧的安全工程师。<br>


👋[获取秘籍](https://madrabbit-vul.github.io/wiki/)<br>

### 1.1 一点说明

在 Anthropic 的 Mythos 一个月完成 1 万+ 漏洞发现的时代，学习漏洞底层的原理还有用吗？

答案很简单：当你面试一个安全工程师岗位的时候，能够深层次理解安全漏洞原理的人会被选择，而只会用工具的人不会。

**你是谁，决定了你怎么做事。Don't be a script kid, nor an AI kid.**

### 1.2 漏洞类型覆盖

系统覆盖以下 13 类 Web 安全漏洞，共计 50+ 个挑战关卡：

| 漏洞类型 | 说明 |
|----------|------|
| 认证与会话安全 | 暴力破解、JWT 安全、密码重置缺陷 |
| SQL 注入 | 登录绕过、UNION 注入、MyBatis 系列注入、盲注 |
| XXE | 文件读取、OOB 外带、Content-Type 切换 |
| XSS | 反射型、存储型、DOM 型 XSS |
| CSRF | GET/POST 型 CSRF、Token 绕过 |
| SSRF | 基础 SSRF、协议走私、绕过技巧、重定向链 |
| 命令执行 | 命令注入、代码执行、绕过技巧 |
| 访问控制 | 水平/垂直越权、IDOR |
| 文件操作 | 文件上传、路径遍历、文件包含 |
| 反序列化 | Java 原生反序列化、Fastjson RCE、Log4Shell |
| 安全配置 | Actuator 泄露、Swagger 泄露 |
| 敏感信息泄露 | 错误泄露、Git 泄露、硬编码密钥 |
| 业务逻辑 | 优惠券滥用、价格篡改、流程跳过 |

---

## 2. 环境依赖

在开始部署之前，请确保本机已安装以下软件：

| 依赖 | 最低版本 | 用途 | 验证命令 |
|------|---------|------|---------|
| JDK | 17+ | 运行 Java 应用 | `java -version` |
| Maven | 3.6+ | 构建项目 | `mvn -version` |
| MySQL | 8.0+ | 数据库服务 | `mysql --version` |
| IDE | 任意 | 源码调试 | 任意你喜欢的 IDE |

---

## 3. 源码部署步骤

> **提示**：为了从源码层面深入理解和掌握漏洞原理,强烈建议你用源码部署本项目,并在IDE中运行和启动本项目。
> **提示**：练靶场最没用的就是搞个payload打一下拿到flag,原理没搞懂,没鸟用~

### 3.1 获取源码

```bash
git clone https://github.com/zhuifengshaonianhanlu/MadRabbit.git
cd MadRabbit
```

### 3.2 启动 MySQL 并创建数据库

确保 MySQL 服务已启动：

```bash
# macOS (Homebrew)
brew services start mysql

# Linux (systemd)
sudo systemctl start mysql

# 验证连接
mysql -u root -p -e "SELECT VERSION();"
```

执行一键初始化脚本，创建数据库、所有表结构和初始数据：

```bash
mysql -u root -p < sql/install_init.sql
```

该脚本会自动完成：
- 创建 `madrabbit` 数据库（UTF-8MB4 编码）
- 创建 6 张业务表（users, flags, challenge_users, logs_access, logs_error, merchant_orders）
- 插入所有初始化数据（用户、关卡 Flag、注入关卡数据、越权订单数据等）

> **提示**：脚本使用 `INSERT IGNORE` 语法，可安全重复执行，不会因主键冲突报错。

验证初始化结果：

```bash
mysql -u root -p madrabbit -e "SHOW TABLES; SELECT COUNT(*) AS user_count FROM users; SELECT COUNT(*) AS flag_count FROM flags;"
```

预期输出应包含 6 张表、6 个用户、50+ 条 Flag 记录,则表示正常.

### 3.3 配置数据库连接

编辑配置文件 `src/main/resources/application.yml`，修改数据库连接信息：

```yaml
spring:
  datasource:
    url: jdbc:mysql://127.0.0.1:3306/madrabbit?useSSL=false&serverTimezone=UTC&characterEncoding=utf8
    username: YOUR_DB_USERNAME     # 修改为你的 MySQL 用户名
    password: YOUR_DB_PASSWORD     # 修改为你的 MySQL 密码
```

### 3.4 编译项目

> **提示**：建议你直接用 IDE 打开项目源码，在 IDE 里面编译和启动。下面是手动使用 Maven 命令的步骤：

```bash
mvn clean package -DskipTests
```

首次构建会下载依赖，耗时较长。构建成功后会在 `target/` 目录下生成 `madrabbit-1.0.0.jar`。

> **常见问题**：如果下载依赖缓慢，可配置国内 Maven 镜像（如阿里云镜像），在 `~/.m2/settings.xml` 中添加镜像配置。

### 3.5 启动应用

**方式一：使用 Maven 直接运行（开发推荐）**

```bash
mvn spring-boot:run
```

**方式二：运行 JAR 包（部署推荐）**

```bash
java -jar target/madrabbit-1.0.0.jar
```

> **注意**：由于项目中反序列化关卡（Log4Shell/Java 反序列化）需要 JVM 开放模块访问权限，`pom.xml` 中已通过 `spring-boot-maven-plugin` 的 `jvmArguments` 配置了必要的 `--add-opens` 参数。使用 `mvn spring-boot:run` 启动时会自动生效；如使用 `java -jar` 方式启动，需手动添加这些参数：
>
> ```bash
> java --add-opens java.naming/javax.naming=ALL-UNNAMED \
>      --add-opens java.base/java.lang=ALL-UNNAMED \
>      --add-opens java.base/java.lang.reflect=ALL-UNNAMED \
>      -jar target/madrabbit-1.0.0.jar
> ```

### 3.6 验证部署

启动成功后，控制台会输出类似以下日志：

```
Started MadrabbitApplication in X.XXX seconds
```

访问以下地址确认服务正常：

| 地址 | 说明 |
|------|------|
| http://localhost:8080/login.html | 系统登录入口 |

---

## 4. 默认测试账号

| 用户名 | 密码 | 角色 |
|--------|------|------|
| admin | 123456 | ADMIN |
| jack | 123456 | LEARNER |
| lucy | 123456 | LEARNER |
| tom | 123456 | LEARNER |

登录后即可浏览漏洞类型列表并进入各挑战关卡。

---

## 5. 技术栈

- **后端**：Java 17 + Spring Boot 2.7 + MyBatis
- **数据库**：MySQL 8.0+
- **前端**：HTML + CSS + JavaScript（原生）
- **构建工具**：Maven

---

## 6. 安全警告

> **警告**：本项目包含**故意设计的安全漏洞**，仅用于教学演示目的。
> **严禁**将本项目部署于生产环境或暴露于公共互联网，否则由此产生的一切安全风险及后果由使用者自行承担。

---

## 7. 许可证

本项目采用自定义许可证。详情请参阅 [LICENSE](LICENSE) 文件。

- 可用于个人学习和教育培训目的
- **未经授权，禁止**：修改、商业使用、二次开发或再分发

---

<p align="center"><b>让我们永远保持激情❤️‍🔥~</b></p>

