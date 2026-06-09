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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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
        mockMvc.perform(post("/api/auth/admin/organizations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code":"scope_unit","name":"Scope Unit","orgType":"UNIT"}
                                """))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/auth/admin/organizations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"parentId":"org_scope_unit","code":"scope_dept","name":"Scope Department","orgType":"DEPARTMENT"}
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
                                {"username":"scope-user","password":"pass123456","displayName":"Scope User","organizationIds":["org_scope_unit","org_scope_dept"],"roleCodes":["scope_user"]}
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
        mockMvc.perform(get("/api/auth/admin/integration-apps/app_scope_system/secrets"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(1))
                .andExpect(jsonPath("$.data.items[0].secretPrefix").isNotEmpty());
        mockMvc.perform(post("/api/auth/admin/integration-apps/app_scope_system/scopes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"scopeType":"ORGANIZATION","scopeId":"org_scope_unit","permission":"USE"}
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/auth/admin/organizations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].code").value("default_unit"))
                .andExpect(jsonPath("$.data.total").value(4));
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
                new ExternalCallerContext("org_scope_unit", List.of("org_scope_dept"), List.of("scope_user"), "external-user"),
                new RequestAuditContext("127.0.0.1", "JUnit")
        ))
                .hasMessageContaining("APP_DISABLED");
    }

    @Test
    void logicallyDeletesManagedResourcesAndFiltersThemFromLists() throws Exception {
        mockMvc.perform(post("/api/auth/admin/organizations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code":"delete_unit","name":"Delete Unit","orgType":"UNIT"}
                                """))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/auth/admin/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"delete-user","password":"pass123456","displayName":"Delete User","organizationIds":["org_delete_unit"],"roleCodes":["app_user"]}
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/api/auth/admin/organizations/org_delete_unit"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("ORGANIZATION_IN_USE"));

        mockMvc.perform(delete("/api/auth/admin/users/user_delete_user"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("DELETED"));
        mockMvc.perform(get("/api/auth/admin/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[?(@.username=='delete-user')]").isEmpty());

        mockMvc.perform(delete("/api/auth/admin/organizations/org_delete_unit"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("DELETED"));
        mockMvc.perform(get("/api/auth/admin/organizations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[?(@.code=='delete_unit')]").isEmpty());

        mockMvc.perform(post("/api/auth/admin/roles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code":"delete_role","name":"Delete Role","roleType":"BUSINESS"}
                                """))
                .andExpect(status().isOk());
        mockMvc.perform(delete("/api/auth/admin/roles/role_delete_role"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("DELETED"));
        mockMvc.perform(get("/api/auth/admin/roles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[?(@.code=='delete_role')]").isEmpty());

        mockMvc.perform(post("/api/auth/admin/integration-apps")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code":"delete-app","name":"Delete App","appType":"BUSINESS_SYSTEM","authType":"API_KEY"}
                                """))
                .andExpect(status().isOk());
        mockMvc.perform(delete("/api/auth/admin/integration-apps/app_delete_app"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("DELETED"));
        mockMvc.perform(get("/api/auth/admin/integration-apps"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[?(@.code=='delete-app')]").isEmpty());
    }

    @Test
    void updatesRoleCodeFromEditRequest() throws Exception {
        mockMvc.perform(post("/api/auth/admin/roles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code":"editable_role","name":"Editable Role","roleType":"BUSINESS"}
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(put("/api/auth/admin/roles/role_editable_role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code":"edited_role","name":"Edited Role","roleType":"BUSINESS","status":"ACTIVE"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.code").value("edited_role"))
                .andExpect(jsonPath("$.data.name").value("Edited Role"));

        mockMvc.perform(get("/api/auth/admin/roles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[?(@.code=='edited_role')]").isNotEmpty());
    }

    @Test
    void createsLongBotScopeAndListsIt() throws Exception {
        mockMvc.perform(post("/api/auth/admin/integration-apps")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code":"archive-system","name":"Archive System","appType":"BUSINESS_SYSTEM","authType":"API_KEY"}
                                """))
                .andExpect(status().isOk());
        String botId = "bot_8110bba5-7ec5-496a-bd25-28aff829bd9c";
        mockMvc.perform(post("/api/auth/admin/integration-apps/app_archive_system/scopes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"scopeType":"BOT","scopeId":"%s","permission":"USE"}
                                """.formatted(botId)))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/auth/admin/integration-apps/app_archive_system/scopes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(1))
                .andExpect(jsonPath("$.data.items[0].scopeType").value("BOT"))
                .andExpect(jsonPath("$.data.items[0].scopeId").value(botId));
    }

    @Test
    void createsManyBotScopesAndListsThem() throws Exception {
        mockMvc.perform(post("/api/auth/admin/integration-apps")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code":"bulk-scope-app","name":"Bulk Scope App","appType":"BUSINESS_SYSTEM","authType":"API_KEY"}
                                """))
                .andExpect(status().isOk());
        for (int index = 0; index < 25; index++) {
            String botId = "bot_" + java.util.UUID.randomUUID();
            mockMvc.perform(post("/api/auth/admin/integration-apps/app_bulk_scope_app/scopes")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"scopeType":"BOT","scopeId":"%s","permission":"USE"}
                                    """.formatted(botId)))
                    .andExpect(status().isOk());
        }
        mockMvc.perform(get("/api/auth/admin/integration-apps/app_bulk_scope_app/scopes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(25));
    }

    private String extract(String json, String name) {
        String marker = "\"" + name + "\":\"";
        int start = json.indexOf(marker) + marker.length();
        int end = json.indexOf('"', start);
        return json.substring(start, end);
    }
}
