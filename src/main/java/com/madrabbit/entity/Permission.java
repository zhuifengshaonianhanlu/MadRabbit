package com.madrabbit.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 权限实体类
 */
@Data
@Schema(description = "权限信息")
public class Permission {

    @Schema(description = "权限ID", example = "1")
    private Long id;

    @Schema(description = "权限名称", example = "user:add")
    private String permissionName;

    @Schema(description = "权限描述", example = "添加用户权限")
    private String description;

    @Schema(description = "创建时间", example = "2023-01-01 12:00:00")
    private String createTime;

    @Schema(description = "更新时间", example = "2023-01-01 12:00:00")
    private String updateTime;
}