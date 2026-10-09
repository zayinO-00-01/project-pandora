package com.projectpandora.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class PandoraApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @Order(1)
    void contextLoads() {}

    @Test
    @Order(2)
    void healthIsPublic() throws Exception {
        mockMvc.perform(get("/api/v1/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"));
    }

    @Test
    @Order(3)
    void loginDemoStaff() throws Exception {
        mockMvc.perform(
                        post("/api/v1/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"username\":\"staff\",\"password\":\"demo1234\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.role").value("STAFF"));
    }

    @Test
    @Order(4)
    void publicRegisterRemoved() throws Exception {
        mockMvc.perform(
                        post("/api/v1/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"username\":\"x\",\"password\":\"demo1234\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(5)
    void nonAdminForbiddenFromUserAdmin() throws Exception {
        String token = login("staff");
        mockMvc.perform(get("/api/v1/admin/users").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(6)
    void adminCanListUsers() throws Exception {
        String token = login("admin");
        mockMvc.perform(get("/api/v1/admin/users").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].username").exists());
    }

    @Test
    @Order(7)
    void teamLeadCanViewStaffLogsInSubtree() throws Exception {
        String staffToken = login("staff");
        mockMvc.perform(
                        post("/api/v1/logs")
                                .header("Authorization", "Bearer " + staffToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"logDate\":\""
                                                + LocalDate.now()
                                                + "\",\"content\":\"穿透联调\"}"))
                .andExpect(status().isCreated());

        long staffId = userId("staff");
        String leadToken = login("team_lead");
        mockMvc.perform(
                        get("/api/v1/logs?userId=" + staffId)
                                .header("Authorization", "Bearer " + leadToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].content").value("穿透联调"));
    }

    @Test
    @Order(8)
    void staffCannotViewFounderLogs() throws Exception {
        long founderId = userId("founder");
        String staffToken = login("staff");
        mockMvc.perform(
                        get("/api/v1/logs?userId=" + founderId)
                                .header("Authorization", "Bearer " + staffToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(9)
    void adminCannotViewAnyLogs() throws Exception {
        long staffId = userId("staff");
        String adminToken = login("admin");
        mockMvc.perform(
                        get("/api/v1/logs?userId=" + staffId)
                                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/logs").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(10)
    void disabledUserCannotLogin() throws Exception {
        String adminToken = login("admin");
        String username = "disabled_case_" + System.currentTimeMillis();
        MvcResult created =
                mockMvc.perform(
                                post("/api/v1/admin/users")
                                        .header("Authorization", "Bearer " + adminToken)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(
                                                "{\"username\":\""
                                                        + username
                                                        + "\",\"password\":\"demo1234\",\"role\":\"STAFF\",\"disabled\":true}"))
                        .andExpect(status().isCreated())
                        .andReturn();
        Number id = JsonPath.read(created.getResponse().getContentAsString(), "$.id");

        mockMvc.perform(
                        post("/api/v1/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"username\":\""
                                                + username
                                                + "\",\"password\":\"demo1234\"}"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(
                        patch("/api/v1/admin/users/" + id.longValue())
                                .header("Authorization", "Bearer " + adminToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"disabled\":false}"))
                .andExpect(status().isOk());

        mockMvc.perform(
                        post("/api/v1/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"username\":\""
                                                + username
                                                + "\",\"password\":\"demo1234\"}"))
                .andExpect(status().isOk());
    }

    private String login(String username) throws Exception {
        MvcResult result =
                mockMvc.perform(
                                post("/api/v1/auth/login")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(
                                                "{\"username\":\""
                                                        + username
                                                        + "\",\"password\":\"demo1234\"}"))
                        .andExpect(status().isOk())
                        .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.token");
    }

    private long userId(String username) throws Exception {
        String adminToken = login("admin");
        MvcResult result =
                mockMvc.perform(
                                get("/api/v1/admin/users")
                                        .header("Authorization", "Bearer " + adminToken))
                        .andExpect(status().isOk())
                        .andReturn();
        List<Number> ids =
                JsonPath.read(
                        result.getResponse().getContentAsString(),
                        "$[?(@.username=='" + username + "')].id");
        return ids.get(0).longValue();
    }
}
