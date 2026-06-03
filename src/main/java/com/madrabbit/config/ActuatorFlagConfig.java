package com.madrabbit.config;

import com.madrabbit.service.FlagService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;

/**
 * 将数据库中的 flag 注入 Spring Environment，
 * 使 /actuator/env 显示的 flag 与 flag_check 验证的值始终一致。
 */
@Configuration
public class ActuatorFlagConfig {

    @Autowired
    private FlagService flagService;

    @Autowired
    private ConfigurableEnvironment environment;

    @PostConstruct
    public void injectFlagProperty() {
        String flag = flagService.getFlag("security-config", "level1");
        if (flag != null) {
            Map<String, Object> props = new HashMap<>();
            props.put("app.secret-flag", flag);
            props.put("app.internal-api-key", "ak-7f8e9d0c1b2a3456");
            environment.getPropertySources()
                    .addFirst(new MapPropertySource("actuatorChallengeFlags", props));
        }
    }
}
