package com.projectpandora.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

class RestartPersistenceTests {

    @TempDir Path directory;

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void savedLogsAndAdministratorSettingsSurviveApplicationRestart() throws Exception {
        String url = "jdbc:h2:file:" + directory.resolve("restart").toAbsolutePath().toString().replace('\\', '/')
                + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH";
        long logId;
        long staffId;
        String bossUsername;
        JsonNode expectedUsers;

        try (ConfigurableApplicationContext first = start(url)) {
            MockMvc mvc = mvc(first);
            String admin = bearer(mvc, "admin", "demo1234");
            JsonNode staff = login(mvc, "staff", "demo1234");
            staffId = staff.path("userId").asLong();
            MvcResult saved = mvc.perform(post("/api/v1/logs")
                            .header("Authorization", "Bearer " + staff.path("token").asText())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(Map.of("logDate", LocalDate.of(2026, 10, 15).toString(), "content", "重启前保存的工作日志"))))
                    .andExpect(status().isCreated()).andReturn();
            logId = read(saved).path("id").asLong();

            JsonNode boss = read(mvc.perform(post("/api/v1/admin/users").header("Authorization", admin)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(Map.of("username", "restart_boss", "password", "demo1234", "role", "DEPT_HEAD"))))
                    .andExpect(status().isCreated()).andReturn());
            bossUsername = boss.path("username").asText();
            mvc.perform(patch("/api/v1/admin/users/" + staffId).header("Authorization", admin)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(Map.of("role", "TEAM_LEAD", "managerId", boss.path("id").asLong(),
                                    "displayName", "持久化员工名称", "password", "updated1234"))))
                    .andExpect(status().isOk());
            for (String username : new String[] {"team_lead", "leader", "founder"}) {
                long id = login(mvc, username, "demo1234").path("userId").asLong();
                mvc.perform(patch("/api/v1/admin/users/" + id).header("Authorization", admin)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json(Map.of("role", "STAFF", "clearManager", true,
                                        "displayName", "已修改_" + username, "disabled", true))))
                        .andExpect(status().isOk());
            }
            expectedUsers = read(mvc.perform(get("/api/v1/admin/users").header("Authorization", admin))
                    .andExpect(status().isOk()).andReturn());
        }

        try (ConfigurableApplicationContext second = start(url)) {
            MockMvc mvc = mvc(second);
            JsonNode staff = login(mvc, "staff", "updated1234");
            assertThat(staff.path("userId").asLong()).isEqualTo(staffId);
            assertThat(staff.path("role").asText()).isEqualTo("TEAM_LEAD");
            String staffAuth = "Bearer " + staff.path("token").asText();
            mvc.perform(get("/api/v1/logs").param("date", "2026-10-15").header("Authorization", staffAuth))
                    .andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value((int) logId))
                    .andExpect(jsonPath("$[0].content").value("重启前保存的工作日志"));
            mvc.perform(get("/api/v1/logs").param("userId", Long.toString(staffId))
                            .header("Authorization", bearer(mvc, bossUsername, "demo1234")))
                    .andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value((int) logId));
            JsonNode actualUsers = read(mvc.perform(get("/api/v1/admin/users")
                            .header("Authorization", bearer(mvc, "admin", "demo1234")))
                    .andExpect(status().isOk()).andReturn());
            assertThat(actualUsers).isEqualTo(expectedUsers);
            for (String username : new String[] {"team_lead", "leader", "founder"}) {
                mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                                .content(json(Map.of("username", username, "password", "demo1234"))))
                        .andExpect(status().isUnauthorized());
            }
        }
    }

    private ConfigurableApplicationContext start(String url) {
        return new SpringApplicationBuilder(PandoraApplication.class).run(
                "--spring.profiles.active=test", "--spring.datasource.url=" + url,
                "--server.port=0", "--spring.main.banner-mode=off", "--logging.level.root=ERROR");
    }

    private MockMvc mvc(ConfigurableApplicationContext context) {
        return MockMvcBuilders.webAppContextSetup((WebApplicationContext) context)
                .apply(springSecurity()).build();
    }

    private JsonNode login(MockMvc mvc, String username, String password) throws Exception {
        return read(mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("username", username, "password", password))))
                .andExpect(status().isOk()).andReturn());
    }

    private String bearer(MockMvc mvc, String username, String password) throws Exception {
        return "Bearer " + login(mvc, username, password).path("token").asText();
    }

    private String json(Object value) throws Exception {
        return mapper.writeValueAsString(value);
    }

    private JsonNode read(MvcResult result) throws Exception {
        return mapper.readTree(result.getResponse().getContentAsByteArray());
    }
}
