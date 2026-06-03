package com.madrabbit.controller.challenge.infoleak;

import com.madrabbit.service.FlagService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.*;

/**
 * 敏感信息泄露关卡 - Level 1: 错误信息泄露 (Error Message Disclosure)
 * 场景：输入异常值触发详细错误信息，暴露数据库类型、SQL语句、内部路径等
 */
@RestController
@RequestMapping("/api/challenge/info-leak/error")
public class ErrorLeakController {

    @Autowired
    private FlagService flagService;

    // 模拟用户数据
    private static final List<Map<String, Object>> USERS = new ArrayList<>();
    static {
        Map<String, Object> u1 = new LinkedHashMap<>();
        u1.put("id", 1); u1.put("name", "张三"); u1.put("email", "zhangsan@example.com");
        USERS.add(u1);

        Map<String, Object> u2 = new LinkedHashMap<>();
        u2.put("id", 2); u2.put("name", "李四"); u2.put("email", "lisi@example.com");
        USERS.add(u2);

        Map<String, Object> u3 = new LinkedHashMap<>();
        u3.put("id", 3); u3.put("name", "王五"); u3.put("email", "wangwu@example.com");
        USERS.add(u3);

        Map<String, Object> u4 = new LinkedHashMap<>();
        u4.put("id", 4); u4.put("name", "赵六"); u4.put("email", "zhaoliu@company.com");
        USERS.add(u4);

        Map<String, Object> u5 = new LinkedHashMap<>();
        u5.put("id", 5); u5.put("name", "管理员"); u5.put("email", "admin@internal.corp");
        USERS.add(u5);
    }

    /**
     * 用户查询接口 - 正常ID返回用户数据，异常输入触发详细错误
     * GET /api/challenge/info-leak/error/user?id={id}
     */
    @GetMapping("/user")
    public Map<String, Object> getUser(@RequestParam(value = "id", defaultValue = "") String idStr) {
        Map<String, Object> result = new HashMap<>();

        // 更新关卡状态为"进行中"
        try {
            Map<String, Object> status = flagService.getStatus("info-leak", "level1");
            if (status != null && "未开始".equals(status.get("status"))) {
                flagService.updateStatus("info-leak", "level1", "进行中");
            }
        } catch (Exception e) {
            // 忽略状态更新异常
        }

        // 空输入
        if (idStr == null || idStr.trim().isEmpty()) {
            result.put("success", false);
            result.put("error", "Parameter 'id' is required");
            return result;
        }

        // 尝试解析为整数
        int id;
        try {
            id = Integer.parseInt(idStr.trim());
        } catch (NumberFormatException e) {
            // 非数字输入 - 触发"SQL解析错误"泄露
            return buildSqlParseError(idStr);
        }

        // 0 或负数 - 触发"数组越界"错误泄露
        if (id <= 0) {
            return buildArrayIndexError(id);
        }

        // 超大数 - 触发"连接超时"错误泄露
        if (id > 99999) {
            return buildTimeoutError(id);
        }

        // 正常范围但无对应用户(6-99999)
        if (id > 5) {
            result.put("success", false);
            result.put("error", "User not found");
            result.put("code", 404);
            return result;
        }

        // 正常查询 (1-5)
        Map<String, Object> user = USERS.get(id - 1);
        result.put("success", true);
        result.put("user", user);
        return result;
    }

    /**
     * SQL解析错误 - 泄露数据库类型、表结构、内部SQL语句
     */
    private Map<String, Object> buildSqlParseError(String input) {
        String flag = flagService.getFlag("info-leak", "level1");

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", false);
        result.put("error", "Internal Server Error");
        result.put("status", 500);

        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("exception", "com.mysql.cj.jdbc.exceptions.SQLSyntaxErrorException");
        detail.put("message", "You have an error in your SQL syntax; check the manual that corresponds to your MySQL server version for the right syntax to use near '" + input + "' at line 1");
        detail.put("sql_statement", "SELECT id, name, email FROM tbl_users WHERE id = " + input);
        detail.put("database", "MySQL 8.0.32");
        detail.put("connection_pool", "HikariCP 5.0.1");
        detail.put("datasource_url", "jdbc:mysql://192.168.1.100:3306/madx_prod");

        List<String> stackTrace = new ArrayList<>();
        stackTrace.add("at com.madrabbit.dao.UserMapper.selectById(UserMapper.java:45)");
        stackTrace.add("at com.madrabbit.service.UserService.getUser(UserService.java:112)");
        stackTrace.add("at com.madrabbit.controller.UserController.query(UserController.java:67)");
        stackTrace.add("at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)");
        stackTrace.add("at org.springframework.web.servlet.FrameworkServlet.service(FrameworkServlet.java:897)");
        stackTrace.add("at org.apache.catalina.core.ApplicationFilterChain.doFilter(ApplicationFilterChain.java:166)");
        detail.put("stack_trace", stackTrace);

        Map<String, String> serverInfo = new LinkedHashMap<>();
        serverInfo.put("server", "Apache Tomcat/9.0.73");
        serverInfo.put("framework", "Spring Boot 2.7.18");
        serverInfo.put("java_version", "OpenJDK 11.0.19");
        serverInfo.put("os", "Linux 5.15.0-78-generic x86_64");
        serverInfo.put("app_path", "/opt/madx/app/target/madx-platform-1.0.jar");
        detail.put("server_info", serverInfo);

        // flag 隐藏在 debug_trace 字段中
        detail.put("debug_trace", "TraceID: a3f8c2e1-9b47-4d6a-b5c0-" + flag);

        result.put("detail", detail);
        return result;
    }

    /**
     * 数组越界错误 - 泄露内部路径和框架信息
     */
    private Map<String, Object> buildArrayIndexError(int id) {
        String flag = flagService.getFlag("info-leak", "level1");

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", false);
        result.put("error", "Internal Server Error");
        result.put("status", 500);

        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("exception", "java.lang.ArrayIndexOutOfBoundsException");
        detail.put("message", "Index " + (id - 1) + " out of bounds for length 5");
        detail.put("component", "com.madrabbit.service.UserService.getUserFromCache");
        detail.put("cache_type", "Redis Cluster 7.0.11 @ 192.168.1.200:6379");

        List<String> stackTrace = new ArrayList<>();
        stackTrace.add("at com.madrabbit.service.UserService.getUserFromCache(UserService.java:88)");
        stackTrace.add("at com.madrabbit.service.UserService.getUser(UserService.java:105)");
        stackTrace.add("at com.madrabbit.controller.UserController.query(UserController.java:67)");
        detail.put("stack_trace", stackTrace);

        Map<String, String> env = new LinkedHashMap<>();
        env.put("config_path", "/opt/madx/config/application-prod.yml");
        env.put("log_path", "/var/log/madx/app.log");
        env.put("temp_dir", "/tmp/madx-uploads");
        detail.put("environment", env);

        detail.put("debug_trace", "ErrorRef: idx-oob-" + flag);

        result.put("detail", detail);
        return result;
    }

    /**
     * 连接超时错误 - 泄露数据库连接信息
     */
    private Map<String, Object> buildTimeoutError(int id) {
        String flag = flagService.getFlag("info-leak", "level1");

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", false);
        result.put("error", "Service Unavailable");
        result.put("status", 503);

        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("exception", "com.zaxxer.hikari.pool.HikariPool$PoolInitializationException");
        detail.put("message", "Failed to obtain JDBC connection from pool (timeout=30000ms, active=50/50)");
        detail.put("pool_name", "HikariPool-1");

        Map<String, Object> connectionInfo = new LinkedHashMap<>();
        connectionInfo.put("jdbc_url", "jdbc:mysql://192.168.1.100:3306/madx_prod?useSSL=false&serverTimezone=UTC");
        connectionInfo.put("username", "madx_app_user");
        connectionInfo.put("max_pool_size", 50);
        connectionInfo.put("idle_timeout", 600000);
        connectionInfo.put("connection_timeout", 30000);
        detail.put("connection_info", connectionInfo);

        List<String> stackTrace = new ArrayList<>();
        stackTrace.add("at com.zaxxer.hikari.pool.HikariPool.createTimeoutException(HikariPool.java:695)");
        stackTrace.add("at com.zaxxer.hikari.pool.HikariPool.getConnection(HikariPool.java:197)");
        stackTrace.add("at com.madrabbit.dao.UserMapper.selectById(UserMapper.java:42)");
        detail.put("stack_trace", stackTrace);

        detail.put("debug_trace", "PoolExhaust: timeout-" + flag);

        result.put("detail", detail);
        return result;
    }
}
