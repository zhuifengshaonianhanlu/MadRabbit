package com.madrabbit;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * MadRabbit Web漏洞靶场系统后端启动类
 */
@SpringBootApplication
@MapperScan("com.madrabbit.repository")
public class MadrabbitApplication {

    public static void main(String[] args) {
        SpringApplication.run(MadrabbitApplication.class, args);
    }

}