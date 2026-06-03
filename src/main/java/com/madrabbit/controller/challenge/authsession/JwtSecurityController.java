package com.madrabbit.controller.challenge.authsession;

import com.madrabbit.entity.User;
import com.madrabbit.repository.UserRepository;
import com.madrabbit.service.FlagService;
import com.madrabbit.service.UserService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT 令牌安全漏洞挑战控制器
 * 演示弱密钥 JWT 签名可被爆破的安全风险
 * 接口路径：/api/challenge/auth-session/jwt/*
 */
@RestController
@RequestMapping("/api/challenge/auth-session/jwt")
@Tag(name = "认证与会话安全 - JWT 令牌安全", description = "JWT 弱密钥爆破漏洞练习")
public class JwtSecurityController {

    @Autowired
    private UserService userService;
    
    @Autowired
    private FlagService flagService;
    
    @Autowired
    private UserRepository userRepository;

    // 弱密钥 - 这是故意设置的弱点，允许攻击者进行爆破 (仅 9 字符，教学演示用)
    private static final String WEAK_SECRET_KEY = "secret123";
    
    /**
     * 获取填充后的密钥 (满足 HS256 的 256 位要求)
     * 教学目的：使用简单的短密钥，通过重复填充达到算法要求
     */
    private SecretKey getPaddedKey() {
        // 将短密钥填充到 32 字节 (256 位)
        StringBuilder paddedKey = new StringBuilder(WEAK_SECRET_KEY);
        while (paddedKey.length() < 32) {
            paddedKey.append(WEAK_SECRET_KEY);
        }
        return Keys.hmacShaKeyFor(paddedKey.substring(0, 32).getBytes(StandardCharsets.UTF_8));
    }

    /**
     * JWT 登录接口
     * POST /api/challenge/auth-session/jwt/login
     * 使用弱密钥生成 JWT Token
     */
    @PostMapping("/login")
    @Operation(summary = "JWT 登录", description = "用户登录后返回使用弱密钥签名的 JWT Token")
    public ResponseEntity<Map<String, Object>> jwtLogin(@RequestBody Map<String, String> loginRequest) {
        Map<String, Object> result = new HashMap<>();

        try {
            String username = loginRequest.get("username");
            String password = loginRequest.get("password");

            // 参数验证
            if (username == null || username.trim().isEmpty()) {
                result.put("success", false);
                result.put("message", "Username is required");
                return ResponseEntity.badRequest().body(result);
            }

            if (password == null || password.trim().isEmpty()) {
                result.put("success", false);
                result.put("message", "Password is required");
                return ResponseEntity.badRequest().body(result);
            }

            // 查询用户
            User user = userService.findByUsername(username);
            
            // 用户不存在
            if (user == null) {
                result.put("success", false);
                result.put("message", "The current user does not exist");
                return ResponseEntity.status(401).body(result);
            }

            // 验证密码 - 前端传来的是明文密码，需要计算 MD5
            String passwordMd5 = user.getPassword_md5();
            if (passwordMd5 == null || passwordMd5.isEmpty()) {
                // 如果数据库中没有 MD5 密码，则使用明文密码计算 MD5
                passwordMd5 = md5(user.getPassword());
            }
            
            // 计算前端传来密码的 MD5 值
            String inputPasswordMd5 = md5(password);
            
            if (!passwordMd5.equals(inputPasswordMd5)) {
                result.put("success", false);
                result.put("message", "Invalid username or password");
                return ResponseEntity.status(401).body(result);
            }

            // 检查账户状态
            if (!"ACTIVE".equals(user.getStatus())) {
                result.put("success", false);
                result.put("message", "Account is locked or disabled");
                return ResponseEntity.status(401).body(result);
            }

            // 生成 JWT Token - 使用弱密钥 HS256 算法签名
            String token = generateWeakJwtToken(username, user.getRole());
            
            result.put("success", true);
            result.put("message", "Login successful");
            result.put("token", token);

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            e.printStackTrace();
            result.put("success", false);
            result.put("message", "System error, please try again later");
            return ResponseEntity.status(500).body(result);
        }
    }

    /**
     * 获取用户信息接口
     * GET /api/challenge/auth-session/jwt/user_info
     * 验证 JWT Token 并返回用户信息，如果是 tom 且 role 为 learner 则返回 flag
     */
    @GetMapping("/user_info")
    @Operation(summary = "获取用户信息", description = "验证 JWT Token 并返回用户信息")
    public ResponseEntity<Map<String, Object>> getUserInfo(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @CookieValue(value = "jwt_token", required = false) String cookieToken) {
        
        Map<String, Object> result = new HashMap<>();

        try {
            // 从 Header 或 Cookie 中获取 Token
            String token = null;
            if (authorization != null && authorization.startsWith("Bearer ")) {
                token = authorization.substring(7);
            } else if (cookieToken != null && !cookieToken.isEmpty()) {
                token = cookieToken;
            }
            
            if (token == null) {
                result.put("success", false);
                result.put("message", "Authorization token is required");
                return ResponseEntity.status(401).body(result);
            }

            // 验证 JWT Token - 使用弱密钥验证
            Map<String, Object> payload = verifyWeakJwtToken(token);
            
            if (payload == null) {
                result.put("success", false);
                result.put("message", "Invalid or expired token");
                return ResponseEntity.status(401).body(result);
            }

            String username = (String) payload.get("username");
            String role = (String) payload.get("role");

            // 从数据库查询用户邮箱信息
            Map<String, Object> userPayload = new HashMap<>(payload);
            try {
                com.madrabbit.entity.User user = userRepository.findByUsername(username);
                if (user != null) {
                    userPayload.put("email", user.getEmail());
                }
            } catch (Exception e) {
                System.err.println("查询用户邮箱失败：" + e.getMessage());
            }

            // 【漏洞利用点】如果用户名是 tom 且角色是 learner，返回 flag
            if ("tom".equalsIgnoreCase(username) && "learner".equalsIgnoreCase(role)) {
                String flag = flagService.getFlag("auth-session", "level3");
                result.put("success", true);
                result.put("message", "Welcome, tom! You have successfully forged the JWT token.");
                result.put("flag", flag);
                result.put("user", userPayload);
                return ResponseEntity.ok(result);
            }

            // 普通用户返回基本信息
            result.put("success", true);
            result.put("message", "User info retrieved successfully");
            result.put("user", userPayload);
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            e.printStackTrace();
            result.put("success", false);
            result.put("message", "System error, please try again later");
            return ResponseEntity.status(500).body(result);
        }
    }

    /**
     * 生成使用弱密钥的 JWT Token
     */
    private String generateWeakJwtToken(String username, String role) {
        long now = System.currentTimeMillis();
        long expirationTime = 1000 * 60 * 60 * 24; // 24 小时有效期
        
        SecretKey key = getPaddedKey();
        
        return Jwts.builder()
                .setSubject(username)
                .claim("username", username)
                .claim("role", role)
                .setIssuedAt(new java.util.Date(now))
                .setExpiration(new java.util.Date(now + expirationTime))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * 验证使用弱密钥的 JWT Token
     */
    private Map<String, Object> verifyWeakJwtToken(String token) {
        try {
            SecretKey key = getPaddedKey();
            
            io.jsonwebtoken.Claims claims = Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            
            Map<String, Object> payload = new HashMap<>();
            payload.put("username", claims.get("username"));
            payload.put("role", claims.get("role"));
            payload.put("sub", claims.getSubject());
            
            return payload;
        } catch (io.jsonwebtoken.ExpiredJwtException e) {
            System.err.println("JWT 已过期：" + e.getMessage());
            return null;
        } catch (io.jsonwebtoken.security.SignatureException e) {
            System.err.println("JWT 签名无效：" + e.getMessage());
            return null;
        } catch (Exception e) {
            // Token 验证失败
            System.err.println("JWT 验证失败：" + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 计算字符串的 MD5 值
     */
    private String md5(String input) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(input.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5 algorithm not found", e);
        }
    }
}
