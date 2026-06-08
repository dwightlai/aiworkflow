package com.mw.ai.agi.auth.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
                                {"username":"lifecycle-user","displayName":"Lifecycle User Updated","mobile":"13800000000","email":"life@example.com","organizationIds":["org_default_unit"],"roleCodes":["app_user"]}
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

    @Test
    void updatesLocalUserPasswordFromUserEdit() throws Exception {
        mockMvc.perform(post("/api/auth/admin/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"edit-password-user","password":"pass123456","displayName":"Edit Password User","organizationIds":["org_default_unit"],"roleCodes":["app_user"]}
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(put("/api/auth/admin/users/user_edit_password_user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"edit-password-user","displayName":"Edit Password User","password":"editpass123","organizationIds":["org_default_unit"],"roleCodes":["app_user"]}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value("edit-password-user"));

        login("edit-password-user", "pass123456")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("AUTH_LOGIN_FAILED"));
        login("edit-password-user", "editpass123")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.user.username").value("edit-password-user"));
    }

    @Test
    void createsUpdatesAndListsUsersBySortOrder() throws Exception {
        mockMvc.perform(post("/api/auth/admin/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"sort-late","password":"pass123456","displayName":"Sort Late","sortOrder":20,"organizationIds":["org_default_unit"],"roleCodes":["app_user"]}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sortOrder").value(20));

        mockMvc.perform(post("/api/auth/admin/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"sort-early","password":"pass123456","displayName":"Sort Early","sortOrder":5,"organizationIds":["org_default_unit"],"roleCodes":["app_user"]}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sortOrder").value(5));

        mockMvc.perform(put("/api/auth/admin/users/user_sort_late")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"sort-late","displayName":"Sort Late Updated","sortOrder":1,"organizationIds":["org_default_unit"],"roleCodes":["app_user"]}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sortOrder").value(1));

        mockMvc.perform(get("/api/auth/admin/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[?(@.username=='sort-late')].sortOrder").value(contains(1)))
                .andExpect(jsonPath("$.data.items[?(@.username=='sort-early')].sortOrder").value(contains(5)));
    }

    @Test
    void batchUpdatesSortOrdersForUsersInSelectedOrganization() throws Exception {
        mockMvc.perform(post("/api/auth/admin/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"batch-a","password":"pass123456","displayName":"Batch A","sortOrder":30,"organizationIds":["org_default_unit"],"roleCodes":["app_user"]}
                                """))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/auth/admin/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"batch-b","password":"pass123456","displayName":"Batch B","sortOrder":40,"organizationIds":["org_default_unit"],"roleCodes":["app_user"]}
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(put("/api/auth/admin/users/sort-orders")
                        .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"organizationId":"org_default_unit","items":[{"userId":"user_batch_a","sortOrder":2},{"userId":"user_batch_b","sortOrder":1}]}
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[?(@.username=='batch-b')].sortOrder").value(contains(1)))
                .andExpect(jsonPath("$.data.items[?(@.username=='batch-a')].sortOrder").value(contains(2)));

        mockMvc.perform(get("/api/auth/admin/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[?(@.username=='batch-b')].sortOrder").value(contains(1)))
                .andExpect(jsonPath("$.data.items[?(@.username=='batch-a')].sortOrder").value(contains(2)));
    }

    @Test
    void renamesUserAndLoginWithNewUsername() throws Exception {
        mockMvc.perform(post("/api/auth/admin/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"rename-me","password":"pass123456","displayName":"Rename Me","organizationIds":["org_default_unit"],"roleCodes":["app_user"]}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value("rename-me"));

        mockMvc.perform(put("/api/auth/admin/users/user_rename_me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"renamed-user","displayName":"Renamed","organizationIds":["org_default_unit"],"roleCodes":["app_user"]}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value("renamed-user"));

        login("rename-me", "pass123456")
                .andExpect(status().isUnauthorized());
        login("renamed-user", "pass123456")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.user.username").value("renamed-user"));
    }

    private org.springframework.test.web.servlet.ResultActions login(String username, String password) throws Exception {
        return mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"username":"%s","password":"%s"}
                        """.formatted(username, password)));
    }
}
