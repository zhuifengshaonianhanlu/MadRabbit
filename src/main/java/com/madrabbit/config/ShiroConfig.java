package com.madrabbit.config;

import org.springframework.context.annotation.Configuration;

/**
 * 临时禁用Shiro配置以解决启动问题
 *后续需要重新配置Shiro以适配Spring Boot 3.x
 */
@Configuration
public class ShiroConfig {
    //移除所有Shiro配置以解决启动问题
    // TODO: 重新配置Shiro以适配Spring Boot 3.x
}