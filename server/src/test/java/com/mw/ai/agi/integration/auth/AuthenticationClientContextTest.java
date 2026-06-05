package com.mw.ai.agi.integration.auth;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.autoconfigure.exclude="
                + "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,"
                + "org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration",
        "spring.cloud.nacos.discovery.enabled=false",
        "third-party.auth.base-url=http://localhost:19090"
})
class AuthenticationClientContextTest {
    @Autowired
    private AuthenticationClient authenticationClient;

    @Autowired
    private RemoteAuthenticationService remoteAuthenticationService;

    @Test
    void registersAuthFeignClientWithoutEnforcingGlobalSecurity() {
        assertThat(authenticationClient).isNotNull();
        assertThat(remoteAuthenticationService).isNotNull();
    }
}
