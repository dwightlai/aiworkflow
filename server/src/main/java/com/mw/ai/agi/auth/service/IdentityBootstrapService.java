package com.mw.ai.agi.auth.service;

import org.springframework.beans.factory.ObjectProvider;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.mw.ai.agi.auth.persistence.UserEntity;
import com.mw.ai.agi.auth.persistence.UserMapper;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;

@Configuration
public class IdentityBootstrapService {
    @Bean
    ApplicationRunner initializeDefaultAdminPassword(
            ObjectProvider<UserMapper> userMapperProvider,
            PasswordEncoder passwordEncoder,
            @Value("${agi.auth.admin-password:${AGI_ADMIN_PASSWORD:admin123}}") String adminPassword
    ) {
        return args -> {
            UserMapper userMapper = userMapperProvider.getIfAvailable();
            if (userMapper == null) {
                return;
            }
            UserEntity admin = userMapper.selectById("user_admin");
            if (admin != null && admin.getPasswordHash() == null) {
                userMapper.update(null, new LambdaUpdateWrapper<UserEntity>()
                        .eq(UserEntity::getId, "user_admin")
                        .set(UserEntity::getPasswordHash, passwordEncoder.encode(adminPassword))
                        .set(UserEntity::getUpdatedAt, Instant.now()));
            }
        };
    }
}
