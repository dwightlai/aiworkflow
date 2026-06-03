package com.aiworkflow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients(basePackages = "com.aiworkflow")
public class AiWorkflowApplication {
    public static void main(String[] args) {
        SpringApplication.run(AiWorkflowApplication.class, args);
    }
}
