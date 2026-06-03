package com.madrabbit.controller;

import com.madrabbit.entity.User;
import com.madrabbit.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 用户管理控制器
 */
@RestController
@RequestMapping("/api/users")
@Tag(name = "用户管理", description = "用户管理相关接口")
public class UserController {

    @Autowired
    private UserService userService;

    /**
     * 获取所有用户
     */
    @GetMapping
    @Operation(summary = "获取所有用户", description = "获取系统中所有活跃用户的信息")
    public ResponseEntity<Map<String, Object>> getAllUsers() {
        Map<String, Object> result = new HashMap<>();

        try {
            List<User> users = userService.findAll();
            result.put("success", true);
            result.put("data", users);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "获取用户列表失败：" + e.getMessage());
            return ResponseEntity.status(500).body(result);
        }

        return ResponseEntity.ok(result);
    }

    /**
     * 根据ID获取用户
     */
    @GetMapping("/{id}")
    @Operation(summary = "根据ID获取用户", description = "根据用户ID获取用户详细信息")
    public ResponseEntity<Map<String, Object>> getUserById(@PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();

        try {
            java.util.Optional<User> userOptional = userService.findById(id);
            if (userOptional.isPresent()) {
                result.put("success", true);
                result.put("data", userOptional.get());
            } else {
                result.put("success", false);
                result.put("message", "用户不存在");
                return ResponseEntity.status(404).body(result);
            }
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "获取用户信息失败：" + e.getMessage());
            return ResponseEntity.status(500).body(result);
        }

        return ResponseEntity.ok(result);
    }

    /**
     * 创建新用户
     */
    @PostMapping
    @Operation(summary = "创建新用户", description = "创建一个新的用户")
    public ResponseEntity<Map<String, Object>> createUser(@RequestBody User user) {
        Map<String, Object> result = new HashMap<>();

        try {
            // 检查用户名是否已存在
            User existingUser = userService.findByUsername(user.getUsername());
            if (existingUser != null) {
                result.put("success", false);
                result.put("message", "用户名已存在");
                return ResponseEntity.badRequest().body(result);
            }

            int rowsAffected = userService.createUser(user);
            if (rowsAffected > 0) {
                result.put("success", true);
                result.put("message", "用户创建成功");
                result.put("data", user);
            } else {
                result.put("success", false);
                result.put("message", "用户创建失败");
                return ResponseEntity.status(500).body(result);
            }
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "创建用户失败：" + e.getMessage());
            return ResponseEntity.status(500).body(result);
        }

        return ResponseEntity.ok(result);
    }

    /**
     * 更新用户信息
     */
    @PutMapping("/{id}")
    @Operation(summary = "更新用户信息", description = "根据用户ID更新用户信息")
    public ResponseEntity<Map<String, Object>> updateUser(@PathVariable Long id, @RequestBody User user) {
        Map<String, Object> result = new HashMap<>();

        try {
            // 设置ID以便更新
            user.setId(id);

            int rowsAffected = userService.updateUser(user);
            if (rowsAffected > 0) {
                // 如果提交了新密码，同步更新密码
                if (user.getPassword() != null && !user.getPassword().trim().isEmpty()) {
                    String plain = user.getPassword().trim();
                    String md5 = md5Hex(plain);
                    userService.updatePassword(user.getUsername(), plain, md5);
                }

                result.put("success", true);
                result.put("message", "用户信息更新成功");
                result.put("data", user);
            } else {
                result.put("success", false);
                result.put("message", "用户更新失败，可能用户不存在");
                return ResponseEntity.status(404).body(result);
            }
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "更新用户失败：" + e.getMessage());
            return ResponseEntity.status(500).body(result);
        }

        return ResponseEntity.ok(result);
    }

    /**
     * 计算 MD5（用于密码存储）
     */
    private String md5Hex(String input) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(input.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("MD5 computation failed", e);
        }
    }

    /**
     * 删除用户
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除用户", description = "根据用户ID删除用户（软删除）")
    public ResponseEntity<Map<String, Object>> deleteUser(@PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();

        try {
            int rowsAffected = userService.deleteUser(id);
            if (rowsAffected > 0) {
                result.put("success", true);
                result.put("message", "用户删除成功");
            } else {
                result.put("success", false);
                result.put("message", "用户删除失败，可能用户不存在");
                return ResponseEntity.status(404).body(result);
            }
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "删除用户失败：" + e.getMessage());
            return ResponseEntity.status(500).body(result);
        }

        return ResponseEntity.ok(result);
    }

    /**
     * 根据角色获取用户
     */
    @GetMapping("/role/{role}")
    @Operation(summary = "根据角色获取用户", description = "根据角色获取对应的所有用户")
    public ResponseEntity<Map<String, Object>> getUsersByRole(@PathVariable String role) {
        Map<String, Object> result = new HashMap<>();

        try {
            List<User> users = userService.findByRole(role);
            result.put("success", true);
            result.put("data", users);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "获取用户列表失败：" + e.getMessage());
            return ResponseEntity.status(500).body(result);
        }

        return ResponseEntity.ok(result);
    }
}