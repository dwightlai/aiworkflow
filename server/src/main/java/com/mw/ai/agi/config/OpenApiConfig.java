package com.mw.ai.agi.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.LinkedHashMap;
import java.util.Map;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI aiWorkflowOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("AI Workflow Platform API")
                        .version("v1")
                        .description("AI Workflow 平台 API 文档。第三方集成请查看「开放 API」分组。")
                        .contact(new Contact().name("AGI Platform").email("support@example.com")))
                .addServersItem(new Server().url("/").description("当前环境"));
    }

    @Bean
    public GroupedOpenApi openApiGroup() {
        return GroupedOpenApi.builder()
                .group("open-api")
                .displayName("开放 API（第三方集成）")
                .pathsToMatch("/api/open/**")
                .addOpenApiCustomizer(openApi -> {
                    openApi.info(new Info()
                            .title("AGI 开放 API")
                            .version("v1")
                            .description("""
                                    供数字档案馆等第三方业务系统调用的开放接口。

                                    认证方式：请求头携带 `X-AGI-App-Code` 与 `X-AGI-Api-Key`。
                                    可选上下文：`X-AGI-User-Id`、`X-AGI-Unit-Id`、`X-AGI-Department-Ids`、`X-AGI-Role-Ids`。
                                    """));
                    Components components = openApi.getComponents();
                    if (components == null) {
                        components = new Components();
                        openApi.components(components);
                    }
                    Map<String, SecurityScheme> securitySchemes = components.getSecuritySchemes();
                    if (securitySchemes == null) {
                        securitySchemes = new LinkedHashMap<>();
                        components.securitySchemes(securitySchemes);
                    }
                    securitySchemes.putAll(openApiSecurityComponents().getSecuritySchemes());
                    openApi.addSecurityItem(new SecurityRequirement().addList("AppCode").addList("ApiKey"));
                })
                .build();
    }

    @Bean
    public GroupedOpenApi managementApiGroup() {
        return GroupedOpenApi.builder()
                .group("management")
                .displayName("管理 API")
                .pathsToMatch("/api/**")
                .pathsToExclude("/api/open/**")
                .build();
    }

    private Components openApiSecurityComponents() {
        return new Components()
                .addSecuritySchemes("AppCode", new SecurityScheme()
                        .type(SecurityScheme.Type.APIKEY)
                        .in(SecurityScheme.In.HEADER)
                        .name("X-AGI-App-Code")
                        .description("第三方应用编码"))
                .addSecuritySchemes("ApiKey", new SecurityScheme()
                        .type(SecurityScheme.Type.APIKEY)
                        .in(SecurityScheme.In.HEADER)
                        .name("X-AGI-Api-Key")
                        .description("第三方应用 API Key"))
                .addSecuritySchemes("UserId", new SecurityScheme()
                        .type(SecurityScheme.Type.APIKEY)
                        .in(SecurityScheme.In.HEADER)
                        .name("X-AGI-User-Id")
                        .description("调用方用户 ID（审计）"))
                .addSecuritySchemes("UnitId", new SecurityScheme()
                        .type(SecurityScheme.Type.APIKEY)
                        .in(SecurityScheme.In.HEADER)
                        .name("X-AGI-Unit-Id")
                        .description("调用方单位 ID"))
                .addSecuritySchemes("DepartmentIds", new SecurityScheme()
                        .type(SecurityScheme.Type.APIKEY)
                        .in(SecurityScheme.In.HEADER)
                        .name("X-AGI-Department-Ids")
                        .description("调用方部门 ID，逗号分隔"))
                .addSecuritySchemes("RoleIds", new SecurityScheme()
                        .type(SecurityScheme.Type.APIKEY)
                        .in(SecurityScheme.In.HEADER)
                        .name("X-AGI-Role-Ids")
                        .description("调用方角色 ID，逗号分隔"));
    }
}
