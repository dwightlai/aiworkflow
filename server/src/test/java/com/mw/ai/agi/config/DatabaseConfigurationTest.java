package com.mw.ai.agi.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySourcesPropertyResolver;
import org.springframework.core.io.ClassPathResource;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class DatabaseConfigurationTest {
    @Test
    void defaultApplicationConfigurationKeepsPostgresqlMigrationLocation() throws Exception {
        PropertySourcesPropertyResolver resolver = resolverFor("application.yml", Map.of());

        assertThat(resolver.getProperty("spring.datasource.url"))
                .isEqualTo("jdbc:postgresql://localhost:5432/aiworkflow");
        assertThat(resolver.getProperty("spring.datasource.username")).isEqualTo("aiworkflow");
        assertThat(resolver.getProperty("spring.datasource.password")).isEqualTo("aiworkflow");
        assertThat(resolver.getProperty("spring.flyway.locations"))
                .isEqualTo("classpath:db/migration/postgresql");
    }

    @Test
    void defaultApplicationConfigurationAcceptsDatasourceOverrides() throws Exception {
        PropertySourcesPropertyResolver resolver = resolverFor("application.yml", Map.of(
                "POSTGRES_JDBC_URL", "jdbc:postgresql://db.example:5432/custom",
                "POSTGRES_USERNAME", "custom_user",
                "POSTGRES_PASSWORD", "custom_password",
                "FLYWAY_LOCATIONS", "classpath:custom/location"
        ));

        assertThat(resolver.getProperty("spring.datasource.url"))
                .isEqualTo("jdbc:postgresql://db.example:5432/custom");
        assertThat(resolver.getProperty("spring.datasource.username")).isEqualTo("custom_user");
        assertThat(resolver.getProperty("spring.datasource.password")).isEqualTo("custom_password");
        assertThat(resolver.getProperty("spring.flyway.locations")).isEqualTo("classpath:custom/location");
    }

    @Test
    void damengProfilePointsAtDamengDriverAndMigrationLocation() throws Exception {
        PropertySourcesPropertyResolver resolver = resolverFor("application-dameng.yml", Map.of());

        assertThat(resolver.getProperty("spring.datasource.url"))
                .isEqualTo("jdbc:dm://localhost:5236/AIWORKFLOW");
        assertThat(resolver.getProperty("spring.datasource.driver-class-name"))
                .isEqualTo("dm.jdbc.driver.DmDriver");
        assertThat(resolver.getProperty("spring.flyway.locations"))
                .isEqualTo("classpath:db/migration/dameng");
    }

    private PropertySourcesPropertyResolver resolverFor(String resourceName, Map<String, Object> overrides) throws Exception {
        MutablePropertySources propertySources = new MutablePropertySources();
        propertySources.addFirst(new MapPropertySource("test-overrides", overrides));
        propertySources.addLast(new YamlPropertySourceLoader()
                .load(resourceName, new ClassPathResource(resourceName))
                .get(0));
        return new PropertySourcesPropertyResolver(propertySources);
    }
}
