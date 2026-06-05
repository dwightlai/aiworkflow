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
        "spring.datasource.url=jdbc:h2:mem:auth-admin-user-lifecycle;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.flyway.locations=classpath:db/migration/postgresql",
        "spring.cloud.nacos.discovery.enabled=false",
        "agi.auth.admin-password=admin123",
        "agi.auth.jwt-secret=test-jwt-secret-with-enough-length"
})
@AutoConfigureMockMvc
class AuthAdminUserLifecycleIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Test
    void updatesUserStatusAndResetsLocalUserPassword() throws Exception {
        mockMvc.perform(post("/api/auth/admin/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"lifecycle-user","password":"pass123456","displayName":"Lifecycle User","organizationIds":["org_default_unit"],"roleCodes":["app_user"]}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value("user_lifecycle_user"));

        mockMvc.perform(put("/api/auth/admin/users/user_lifecycle_user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"displayName":"Lifecycle User Updated","mobile":"13800000000","email":"life@example.com","organizationIds":["org_default_unit"],"roleCodes":["app_user"]}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.displayName").value("Lifecycle User Updated"))
                .andExpect(jsonPath("$.data.organizationIds[0]").value("org_default_unit"));

        login("lifecycle-user", "pass123456")
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/admin/users/user_lifecycle_user/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"DISABLED"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("DISABLED"));

        login("lifecycle-user", "pass123456")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("AUTH_USER_DISABLED"));

        mockMvc.perform(post("/api/auth/admin/users/user_lifecycle_user/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"LOCKED"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("LOCKED"));

        login("lifecycle-user", "pass123456")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("AUTH_USER_LOCKED"));

        mockMvc.perform(post("/api/auth/admin/users/user_lifecycle_user/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"ACTIVE"}
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/admin/users/user_lifecycle_user/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"password":"newpass123"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value("lifecycle-user"));

        login("lifecycle-user", "pass123456")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("AUTH_LOGIN_FAILED"));

        login("lifecycle-user", "newpass123")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.user.username").value("lifecycle-user"));
    }

    private org.springframework.test.web.servlet.ResultActions login(String username, String password) throws Exception {
        return mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"username":"%s","password":"%s"}
                        """.formatted(username, password)));
    }
}
