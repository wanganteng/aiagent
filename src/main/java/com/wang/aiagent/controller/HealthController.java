package com.wang.aiagent.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/health")
public class HealthController {

    /**
     * 测试Knife4j文档生成工具
     * @return
     */
    @GetMapping
    public String healthCheck() {
        return "ok";
    }
}

