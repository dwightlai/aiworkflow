package com.mw.ai.agi.openapi.client.autoconfigure;

import com.mw.ai.agi.openapi.client.AgiOpenApiHeaders;
import com.mw.ai.agi.openapi.client.AgiOpenApiRequestContext;
import com.mw.ai.agi.openapi.client.AgiOpenApiRequestContextHolder;
import feign.RequestInterceptor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;

import java.util.StringJoiner;

@AutoConfiguration
@ConditionalOnClass(RequestInterceptor.class)
@ConditionalOnProperty(prefix = "agi.openapi", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(AgiOpenApiProperties.class)
@EnableFeignClients(basePackages = "com.mw.ai.agi.openapi.client.feign")
public class AgiOpenApiClientAutoConfiguration {

    @Bean
    public RequestInterceptor agiOpenApiRequestInterceptor(AgiOpenApiProperties properties) {
        return template -> {
            if (properties.getAppCode() != null && !properties.getAppCode().isBlank()) {
                template.header(AgiOpenApiHeaders.APP_CODE, properties.getAppCode());
            }
            if (properties.getApiKey() != null && !properties.getApiKey().isBlank()) {
                template.header(AgiOpenApiHeaders.API_KEY, properties.getApiKey());
            }
            AgiOpenApiRequestContext context = AgiOpenApiRequestContextHolder.get();
            if (context == null) {
                return;
            }
            if (context.userId() != null && !context.userId().isBlank()) {
                template.header(AgiOpenApiHeaders.USER_ID, context.userId());
            }
            if (context.unitId() != null && !context.unitId().isBlank()) {
                template.header(AgiOpenApiHeaders.UNIT_ID, context.unitId());
            }
            if (context.departmentIds() != null && !context.departmentIds().isEmpty()) {
                template.header(AgiOpenApiHeaders.DEPARTMENT_IDS, join(context.departmentIds()));
            }
            if (context.roleIds() != null && !context.roleIds().isEmpty()) {
                template.header(AgiOpenApiHeaders.ROLE_IDS, join(context.roleIds()));
            }
        };
    }

    private static String join(Iterable<String> values) {
        StringJoiner joiner = new StringJoiner(",");
        values.forEach(joiner::add);
        return joiner.toString();
    }
}
