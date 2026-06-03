package com.madrabbit.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 角色实体类
 */
@Data
@Schema(description = "角色信息")
public class Role {

    @Schema(description = "角色ID", example = "1")
    private Long id;

    @Schema(description = "角色名称", example = "ADMIN")
    private String roleName;

    @Schema(description = "角色描述", example = "超级管理员")
    private String description;

    @Schema(description = "权限列表", example = "user:add,user:delete")
    private String permissions;

    @Schema(description = "创建时间", example = "2023-01-01 12:00:00")
    private String createTime;

    @Schema(description = "更新时间", example = "2023-01-01 12:00:00")
    private String updateTime;
}