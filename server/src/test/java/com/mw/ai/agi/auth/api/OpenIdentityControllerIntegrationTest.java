package com.mw.ai.agi.auth.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:open-identity;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.flyway.locations=classpath:db/migration/postgresql",
        "spring.cloud.nacos.discovery.enabled=false",
        "agi.auth.admin-password=admin123"
})
@AutoConfigureMockMvc
class OpenIdentityControllerIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Test
    void resolvesRuntimeIdentityFromApiKeyHeadersAndBodyContext() throws Exception {
        mockMvc.perform(post("/api/auth/admin/units")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code":"open_unit","name":"Open Unit","unitType":"BUSINESS_ORG"}
                                """))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/auth/admin/integration-apps")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code":"open-system","name":"Open System","appType":"BUSINESS_SYSTEM","authType":"API_KEY"}
                                """))
                .andExpect(status().isOk());
        String secretResponse = mockMvc.perform(post("/api/auth/admin/integration-apps/app_open_system/secrets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.apiKey", containsString("agi_")))
                .andReturn()
                .getResponse()
                .getContentAsString();
        String apiKey = extract(secretResponse, "apiKey");
        mockMvc.perform(post("/api/auth/admin/integration-apps/app_open_system/scopes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"scopeType":"UNIT","scopeId":"unit_open_unit","permission":"USE"}
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/open/identity/resolve")
                        .header("X-AGI-App-Code", "open-system")
                        .header("X-AGI-Api-Key", apiKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"unitId":"unit_open_unit","departmentIds":["dept_external"],"roleIds":["open_user"],"userId":"external-42"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.tenantId").value("tenant_default"))
                .andExpect(jsonPath("$.data.appId").value("app_open_system"))
                .andExpect(jsonPath("$.data.userId").value("external-42"))
                .andExpect(jsonPath("$.data.activeUnitId").value("unit_open_unit"))
                .andExpect(jsonPath("$.data.departmentIds[0]").value("dept_external"))
                .andExpect(jsonPath("$.data.roleIds[0]").value("open_user"))
                .andExpect(jsonPath("$.data.authType").value("API_KEY"));
    }

    private String extract(String json, String name) {
        String marker = "\"" + name + "\":\"";
        int start = json.indexOf(marker) + marker.length();
        int end = json.indexOf('"', start);
        return json.substring(start, end);
    }
}
