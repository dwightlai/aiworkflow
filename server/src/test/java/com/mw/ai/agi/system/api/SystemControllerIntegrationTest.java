package com.mw.ai.agi.system.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:system-api;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.flyway.locations=classpath:db/migration/postgresql",
        "spring.cloud.nacos.discovery.enabled=false"
})
@AutoConfigureMockMvc
class SystemControllerIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void managesMenusDictionariesAndAuditLogs() throws Exception {
        mockMvc.perform(get("/api/system/menus/navigation"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].title").exists());

        String menuResponse = mockMvc.perform(post("/api/system/menus")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "groupTitle":"测试分组",
                                  "menuKey":"test-menu",
                                  "title":"测试菜单",
                                  "path":"/test-menu",
                                  "sortOrder":99,
                                  "visible":true,
                                  "platformOnly":false,
                                  "status":"ENABLED"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.menuKey").value("test-menu"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        String menuId = objectMapper.readTree(menuResponse).path("data").path("id").asText();

        mockMvc.perform(put("/api/system/menus/{id}", menuId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "groupTitle":"测试分组",
                                  "menuKey":"test-menu",
                                  "title":"测试菜单更新",
                                  "path":"/test-menu",
                                  "sortOrder":100,
                                  "visible":true,
                                  "platformOnly":false,
                                  "status":"ENABLED"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("测试菜单更新"));

        String dictResponse = mockMvc.perform(post("/api/system/dictionaries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "code":"test_status",
                                  "name":"测试状态",
                                  "description":"测试字典",
                                  "status":"ENABLED"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.code").value("test_status"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        String dictId = objectMapper.readTree(dictResponse).path("data").path("id").asText();

        mockMvc.perform(post("/api/system/dictionaries/{id}/items", dictId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "label":"启用",
                                  "value":"ON",
                                  "sortOrder":1,
                                  "status":"ENABLED"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.value").value("ON"));

        mockMvc.perform(get("/api/system/dictionaries/code/test_status/items"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].value").value("ON"));

        mockMvc.perform(get("/api/system/audit-logs")
                        .param("eventType", "SYSTEM_MENU_CREATED")
                        .param("page", "1")
                        .param("pageSize", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].eventType").value("SYSTEM_MENU_CREATED"));

        mockMvc.perform(delete("/api/system/menus/{id}", menuId))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/api/system/dictionaries/{id}", dictId))
                .andExpect(status().isOk());
    }

    @Test
    void rejectsInvalidMenuInput() throws Exception {
        mockMvc.perform(post("/api/system/menus")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "groupTitle":"测试",
                                  "menuKey":"Invalid Key",
                                  "title":"测试",
                                  "path":"no-leading-slash",
                                  "status":"ENABLED"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }
}
