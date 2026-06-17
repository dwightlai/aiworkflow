package com.mw.ai.agi.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(AgiStorageProperties.class)
public class AgiStorageConfig {
}
