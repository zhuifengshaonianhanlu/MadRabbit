package com.madrabbit.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 用户实体类
 */
@Schema(description = "用户信息")
public class User {

    @Schema(description = "用户ID", example = "1")
    private Long id;

    @Schema(description = "用户名", example = "admin")
    private String username;

    @Schema(description = "密码", example = "password")
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;

    @Schema(description = "密码MD5", example = "5f4dcc3b5aa765d61d8327deb882cf99")
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password_md5;

    @Schema(description = "邮箱", example = "admin@example.com")
    private String email;

    @Schema(description = "用户角色", example = "ADMIN")
    private String role;

    @Schema(description = "用户状态", example = "ACTIVE")
    private String status;

    @Schema(description = "创建时间", example = "2023-01-01 12:00:00")
    private String createTime;

    @Schema(description = "更新时间", example = "2023-01-01 12:00:00")
    private String updateTime;

    // Getter and Setter methods
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getPassword_md5() {
        return password_md5;
    }

    public void setPassword_md5(String password_md5) {
        this.password_md5 = password_md5;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getCreateTime() {
        return createTime;
    }

    public void setCreateTime(String createTime) {
        this.createTime = createTime;
    }

    public String getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(String updateTime) {
        this.updateTime = updateTime;
    }
}