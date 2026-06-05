package com.mw.ai.agi.integration.organization;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.autoconfigure.exclude="
                + "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,"
                + "org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration",
        "spring.cloud.nacos.discovery.enabled=false"
})
class OrganizationClientContextTest {
    @Autowired
    private OrganizationClient organizationClient;

    @Test
    void registersOrganizationFeignClientBean() {
        assertThat(organizationClient).isNotNull();
    }
}
