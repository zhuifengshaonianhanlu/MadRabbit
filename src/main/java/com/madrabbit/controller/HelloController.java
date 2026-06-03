package com.madrabbit.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Hello World控制器，用于验证项目初始化
 */
@RestController
public class HelloController {

    @GetMapping("/hello")
    public String hello() {
        return "Welcome to VulnHub - Web Vulnerability Practice Platform!";
    }
}