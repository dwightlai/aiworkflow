package com.mw.ai.agi.auth.api;

import com.mw.ai.agi.auth.service.ExternalCallerContext;
import com.mw.ai.agi.auth.service.IntegrationAppAuthenticator;
import com.mw.ai.agi.auth.service.RequestAuditContext;
import com.mw.ai.agi.auth.service.RuntimeIdentityContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:auth-admin;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.flyway.locations=classpath:db/migration/postgresql",
        "spring.cloud.nacos.discovery.enabled=false",
        "agi.auth.admin-password=admin123",
        "agi.auth.jwt-secret=test-jwt-secret-with-enough-length"
})
@AutoConfigureMockMvc
class AuthAdminControllerIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private IntegrationAppAuthenticator authenticator;

    @Test
    void managesOrganizationUsersAndIntegrationApps() throws Exception {
        mockMvc.perform(get("/api/auth/admin/tenants"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].code").value("default"));

        mockMvc.perform(post("/api/auth/admin/units")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code":"archive_unit","externalUnitId":"ARCHIVE-001","name":"Archive Unit","unitType":"ARCHIVE_ORG"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value("unit_archive_unit"))
                .andExpect(jsonPath("$.data.code").value("archive_unit"));

        mockMvc.perform(post("/api/auth/admin/departments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"unitId":"unit_archive_unit","code":"archive_dept","externalDepartmentId":"D-001","name":"Archive Department","sortOrder":10}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value("dept_archive_dept"));

        mockMvc.perform(post("/api/auth/admin/roles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"unitId":"unit_archive_unit","code":"archive_user","name":"Archive User","roleType":"BUSINESS","externalRoleId":"R-001"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.code").value("archive_user"));

        mockMvc.perform(post("/api/auth/admin/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"archive-admin","password":"pass123456","displayName":"Archive Admin","unitIds":["unit_archive_unit"],"departmentIds":["dept_archive_dept"],"roleCodes":["archive_user"]}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value("archive-admin"))
                .andExpect(jsonPath("$.data.unitIds[0]").value("unit_archive_unit"))
                .andExpect(jsonPath("$.data.departmentIds[0]").value("dept_archive_dept"))
                .andExpect(jsonPath("$.data.roleIds[0]").value("archive_user"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"archive-admin","password":"pass123456"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.user.activeUnitId").value("unit_archive_unit"))
                .andExpect(jsonPath("$.data.user.roleIds[0]").value("archive_user"));

        mockMvc.perform(post("/api/auth/admin/integration-apps")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code":"archive-system","name":"Archive System","appType":"ARCHIVE_SYSTEM","authType":"API_KEY"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value("app_archive_system"));

        String secretResponse = mockMvc.perform(post("/api/auth/admin/integration-apps/app_archive_system/secrets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"secretName":"default"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.apiKey", containsString("agi_")))
                .andReturn()
                .getResponse()
                .getContentAsString();
        String apiKey = extract(secretResponse, "apiKey");

        mockMvc.perform(post("/api/auth/admin/integration-apps/app_archive_system/scopes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"scopeType":"UNIT","scopeId":"unit_archive_unit","permission":"USE"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.scopeId").value("unit_archive_unit"));

        RuntimeIdentityContext context = authenticator.authenticateApiKey(
                "archive-system",
                apiKey,
                new ExternalCallerContext("unit_archive_unit", List.of("dept_archive_dept"), List.of("archive_user"), "external-1"),
                new RequestAuditContext("127.0.0.1", "JUnit")
        );
        assertThat(context.appId()).isEqualTo("app_archive_system");
        assertThat(context.activeUnitId()).isEqualTo("unit_archive_unit");
    }

    private String extract(String json, String name) {
        String marker = "\"" + name + "\":\"";
        int start = json.indexOf(marker) + marker.length();
        int end = json.indexOf('"', start);
        return json.substring(start, end);
    }
}
