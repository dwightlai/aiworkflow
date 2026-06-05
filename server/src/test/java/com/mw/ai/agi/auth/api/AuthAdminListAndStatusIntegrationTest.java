package com.mw.ai.agi.auth.api;

import com.mw.ai.agi.auth.service.ExternalCallerContext;
import com.mw.ai.agi.auth.service.IntegrationAppAuthenticator;
import com.mw.ai.agi.auth.service.RequestAuditContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:auth-admin-list;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.flyway.locations=classpath:db/migration/postgresql",
        "spring.cloud.nacos.discovery.enabled=false",
        "agi.auth.admin-password=admin123"
})
@AutoConfigureMockMvc
class AuthAdminListAndStatusIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private IntegrationAppAuthenticator authenticator;

    @Test
    void listsManagedResourcesAndDisablesIntegrationApp() throws Exception {
        mockMvc.perform(post("/api/auth/admin/units")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code":"scope_unit","name":"Scope Unit","unitType":"BUSINESS_ORG"}
                                """))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/auth/admin/departments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"unitId":"unit_scope_unit","code":"scope_dept","name":"Scope Department"}
                                """))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/auth/admin/roles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code":"scope_user","name":"Scope User","roleType":"BUSINESS"}
                                """))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/auth/admin/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"scope-user","password":"pass123456","displayName":"Scope User","unitIds":["unit_scope_unit"],"departmentIds":["dept_scope_dept"],"roleCodes":["scope_user"]}
                                """))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/auth/admin/integration-apps")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code":"scope-system","name":"Scope System","appType":"BUSINESS_SYSTEM","authType":"API_KEY"}
                                """))
                .andExpect(status().isOk());
        String secretResponse = mockMvc.perform(post("/api/auth/admin/integration-apps/app_scope_system/secrets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String apiKey = extract(secretResponse, "apiKey");
        mockMvc.perform(post("/api/auth/admin/integration-apps/app_scope_system/scopes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"scopeType":"UNIT","scopeId":"unit_scope_unit","permission":"USE"}
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/auth/admin/units"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].code").value("default_unit"))
                .andExpect(jsonPath("$.data.total").value(2));
        mockMvc.perform(get("/api/auth/admin/departments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(2));
        mockMvc.perform(get("/api/auth/admin/roles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].code").value("app_user"));
        mockMvc.perform(get("/api/auth/admin/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(2));
        mockMvc.perform(get("/api/auth/admin/integration-apps"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].code").value("scope-system"));

        mockMvc.perform(post("/api/auth/admin/integration-apps/app_scope_system/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"DISABLED"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("DISABLED"));

        assertThatThrownBy(() -> authenticator.authenticateApiKey(
                "scope-system",
                apiKey,
                new ExternalCallerContext("unit_scope_unit", List.of("dept_scope_dept"), List.of("scope_user"), "external-user"),
                new RequestAuditContext("127.0.0.1", "JUnit")
        ))
                .hasMessageContaining("APP_DISABLED");
    }

    private String extract(String json, String name) {
        String marker = "\"" + name + "\":\"";
        int start = json.indexOf(marker) + marker.length();
        int end = json.indexOf('"', start);
        return json.substring(start, end);
    }
}
