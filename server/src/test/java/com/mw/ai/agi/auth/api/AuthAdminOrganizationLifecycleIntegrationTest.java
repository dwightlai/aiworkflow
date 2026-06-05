package com.mw.ai.agi.auth.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:auth-admin-org-lifecycle;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.flyway.locations=classpath:db/migration/postgresql",
        "spring.cloud.nacos.discovery.enabled=false",
        "agi.auth.admin-password=admin123"
})
@AutoConfigureMockMvc
class AuthAdminOrganizationLifecycleIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Test
    void managesOrganizationsAsTreeNodes() throws Exception {
        mockMvc.perform(post("/api/auth/admin/organizations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code":"design_unit","externalOrgId":"U-001","name":"Design Unit","orgType":"UNIT"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value("org_design_unit"))
                .andExpect(jsonPath("$.data.parentId").doesNotExist())
                .andExpect(jsonPath("$.data.orgType").value("UNIT"));

        mockMvc.perform(post("/api/auth/admin/organizations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"parentId":"org_design_unit","code":"design_dept","externalOrgId":"D-001","name":"Design Department","orgType":"DEPARTMENT","sortOrder":8}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value("org_design_dept"))
                .andExpect(jsonPath("$.data.parentId").value("org_design_unit"))
                .andExpect(jsonPath("$.data.path").value("/org_design_unit/org_design_dept"));

        mockMvc.perform(put("/api/auth/admin/organizations/org_design_dept")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"parentId":"org_default_unit","externalOrgId":"D-002","name":"Design Department Updated","orgType":"DEPARTMENT","sortOrder":12,"status":"DISABLED"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Design Department Updated"))
                .andExpect(jsonPath("$.data.parentId").value("org_default_unit"))
                .andExpect(jsonPath("$.data.status").value("DISABLED"));

        mockMvc.perform(post("/api/auth/admin/roles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"organizationId":"org_design_unit","code":"design_role","name":"Design Role","roleType":"BUSINESS","externalRoleId":"R-001"}
                                """))
                .andExpect(status().isOk());
        mockMvc.perform(put("/api/auth/admin/roles/role_design_role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"organizationId":"org_design_unit","name":"Design Role Updated","roleType":"BUSINESS","externalRoleId":"R-002","status":"DISABLED"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Design Role Updated"))
                .andExpect(jsonPath("$.data.externalRoleId").value("R-002"))
                .andExpect(jsonPath("$.data.status").value("DISABLED"));
    }

    @Test
    void createsUserWithMultipleOrganizationMemberships() throws Exception {
        mockMvc.perform(post("/api/auth/admin/organizations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code":"second_unit","name":"Second Unit","orgType":"UNIT"}
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/admin/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"multi-org-user","password":"pass123456","displayName":"Multi Org User","organizationIds":["org_default_unit","org_second_unit"],"roleCodes":["app_user"]}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.organizationIds[0]").value("org_default_unit"))
                .andExpect(jsonPath("$.data.organizationIds[1]").value("org_second_unit"));
    }
}
