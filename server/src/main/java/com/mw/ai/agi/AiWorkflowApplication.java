package com.mw.ai.agi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients(basePackages = "com.mw.ai.agi")
public class AiWorkflowApplication {
    public static void main(String[] args) {
        SpringApplication.run(AiWorkflowApplication.class, args);
    }
}
