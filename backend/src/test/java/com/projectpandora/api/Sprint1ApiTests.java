package com.projectpandora.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.projectpandora.api.log.WorkLogController;
import com.projectpandora.api.log.WorkLogRepository;
import com.projectpandora.api.security.AccessService;
import com.projectpandora.api.security.JwtService;
import com.projectpandora.api.security.UserPrincipal;
import com.projectpandora.api.user.Role;
import com.projectpandora.api.user.UserRepository;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class Sprint1ApiTests {

    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper mapper;
    @Autowired private UserRepository users;
    @Autowired private WorkLogRepository logs;
    @Value("${pandora.jwt.secret}") private String jwtSecret;

    @ParameterizedTest
    @CsvSource({"admin,ADMIN", "founder,FOUNDER", "leader,DEPT_HEAD", "team_lead,TEAM_LEAD", "staff,STAFF"})
    void fiveRolesCanLogin(String username, String role) throws Exception {
        assertThat(login(username).path("role").asText()).isEqualTo(role);
    }

    @ParameterizedTest
    @ValueSource(strings = {"staff", "unknown_user"})
    void invalidCredentialsDoNotIssueToken(String username) throws Exception {
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("username", username, "password", "incorrect"))))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.token").doesNotExist());
    }

    @ParameterizedTest
    @ValueSource(strings = {"/api/v1/logs", "/api/v1/logs/integrity", "/api/v1/panels/map", "/api/v1/admin/users"})
    void missingTokenReturnsJson401(String path) throws Exception {
        mvc.perform(get(path)).andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(401));
    }

    @ParameterizedTest
    @ValueSource(strings = {"invalid", "a.b.c", ""})
    void malformedTokenReturns401(String token) throws Exception {
        mvc.perform(get("/api/v1/logs").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void expiredAndWrongSignatureTokensAreRejected() throws Exception {
        long id = users.findByUsername("staff").orElseThrow().getId();
        String expired = new JwtService(jwtSecret, -1).createToken(id, "staff", Role.STAFF);
        String wrongSignature = new JwtService("different-test-secret-at-least-32-bytes", 1)
                .createToken(id, "staff", Role.STAFF);
        for (String token : new String[] {expired, wrongSignature}) {
            mvc.perform(get("/api/v1/logs").header("Authorization", "Bearer " + token))
                    .andExpect(status().isUnauthorized());
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"staff", "team_lead", "leader", "founder"})
    void appRolesCannotUseAnyUserAdministrationOperation(String username) throws Exception {
        String auth = bearer(username);
        mvc.perform(get("/api/v1/admin/users").header("Authorization", auth))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.status").value(403));
        mvc.perform(post("/api/v1/admin/users").header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
        mvc.perform(patch("/api/v1/admin/users/1").header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCannotAccessAppEndpointsOrCreateBusinessLogs() throws Exception {
        String auth = bearer("admin");
        long before = logs.count();
        for (String path : new String[] {"/api/v1/logs", "/api/v1/logs/integrity", "/api/v1/panels/map"}) {
            mvc.perform(get(path).header("Authorization", auth)).andExpect(status().isForbidden())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.status").value(403));
        }
        mvc.perform(post("/api/v1/logs").header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("logDate", LocalDate.now().toString(), "content", "禁止写入"))))
                .andExpect(status().isForbidden());
        assertThat(logs.count()).isEqualTo(before);
    }

    @ParameterizedTest
    @ValueSource(strings = {"staff", "team_lead", "leader", "founder"})
    void appRolesCanWriteReadAndSeeTheirTodayPanel(String username) throws Exception {
        JsonNode me = login(username);
        String auth = "Bearer " + me.path("token").asText();
        LocalDate today = LocalDate.now();
        JsonNode saved = createLog(auth, today, "  晨星冒烟测试校验通过  ");
        assertThat(saved.path("userId").asLong()).isEqualTo(me.path("userId").asLong());
        assertThat(saved.path("status").asText()).isEqualTo("submitted");
        assertThat(saved.path("content").asText()).isEqualTo("晨星冒烟测试校验通过");
        mvc.perform(get("/api/v1/logs").param("date", today.toString()).header("Authorization", auth))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(saved.path("id").asInt()));
        mvc.perform(get("/api/v1/panels/map").header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.todayLogs[0].id").value(saved.path("id").asInt()))
                .andExpect(jsonPath("$.companyImportant").isArray())
                .andExpect(jsonPath("$.companyDispatch").isArray())
                .andExpect(jsonPath("$.personalTop").isArray());
        mvc.perform(get("/api/v1/logs/integrity").header("Authorization", auth))
                .andExpect(status().isOk()).andExpect(jsonPath("$.ok").value(true));
    }

    @Test
    void logIsolationDateFilteringAndOrderArePreserved() throws Exception {
        JsonNode first = createUser("STAFF", Map.of());
        JsonNode second = createUser("STAFF", Map.of());
        String auth = bearer(first.path("username").asText());
        String otherAuth = bearer(second.path("username").asText());
        mvc.perform(get("/api/v1/logs/integrity").header("Authorization", auth))
                .andExpect(status().isOk()).andExpect(jsonPath("$.ok").value(true));
        LocalDate day = LocalDate.of(2026, 10, 10);
        createLog(auth, day, "first");
        JsonNode newest = createLog(auth, day, "second");
        createLog(auth, day.minusDays(1), "previous day");
        createLog(otherAuth, day, "private");
        mvc.perform(get("/api/v1/logs").param("date", day.toString()).header("Authorization", auth))
                .andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id").value(newest.path("id").asInt()))
                .andExpect(jsonPath("$[1].content").value("first"));
        mvc.perform(get("/api/v1/logs").param("userId", second.path("id").asText())
                        .header("Authorization", auth)).andExpect(status().isForbidden());
    }

    @ParameterizedTest
    @ValueSource(strings = {"team_lead", "leader", "founder"})
    void superiorsReadStaffThroughTheOrganization(String username) throws Exception {
        long id = users.findByUsername("staff").orElseThrow().getId();
        mvc.perform(get("/api/v1/logs").param("userId", Long.toString(id))
                        .header("Authorization", bearer(username))).andExpect(status().isOk());
    }

    @ParameterizedTest
    @ValueSource(strings = {"team_lead", "leader"})
    void leadsCannotReadOutsideTheirSubtree(String username) throws Exception {
        JsonNode outside = createUser("STAFF", Map.of());
        mvc.perform(get("/api/v1/logs").param("userId", outside.path("id").asText())
                        .header("Authorization", bearer(username))).andExpect(status().isForbidden());
    }

    @Test
    void founderReadsOutsideOwnChainButCannotReadAdmin() throws Exception {
        JsonNode outside = createUser("STAFF", Map.of());
        String auth = bearer("founder");
        mvc.perform(get("/api/v1/logs").param("userId", outside.path("id").asText())
                        .header("Authorization", auth)).andExpect(status().isOk());
        long adminId = users.findByUsername("admin").orElseThrow().getId();
        mvc.perform(get("/api/v1/logs").param("userId", Long.toString(adminId))
                        .header("Authorization", auth)).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/logs").param("userId", Long.toString(Long.MAX_VALUE))
                        .header("Authorization", auth)).andExpect(status().isNotFound());
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"logDate\":\"2026-10-15\",\"content\":\"   \"}",
            "{\"logDate\":\"bad-date\",\"content\":\"text\"}", "{"})
    void invalidLogsReturn400WithoutWrites(String payload) throws Exception {
        long before = logs.count();
        mvc.perform(post("/api/v1/logs").header("Authorization", bearer("staff"))
                        .contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400));
        assertThat(logs.count()).isEqualTo(before);
    }

    @ParameterizedTest
    @ValueSource(strings = {"date=not-a-date", "userId=not-a-number"})
    void invalidLogQueryReturns400(String query) throws Exception {
        mvc.perform(get("/api/v1/logs?" + query).header("Authorization", bearer("staff")))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void logOwnerCannotBeSpoofed() throws Exception {
        JsonNode me = login("staff");
        long founderId = users.findByUsername("founder").orElseThrow().getId();
        JsonNode result = read(mvc.perform(post("/api/v1/logs")
                        .header("Authorization", "Bearer " + me.path("token").asText())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("logDate", LocalDate.now().toString(), "content", "own", "userId", founderId))))
                .andExpect(status().isCreated()).andReturn());
        assertThat(result.path("userId").asLong()).isEqualTo(me.path("userId").asLong());
    }

    @Test
    void disableRejectsOldTokenAndPreservesHistoricalLogs() throws Exception {
        JsonNode user = createUser("STAFF", Map.of());
        String username = user.path("username").asText();
        String auth = bearer(username);
        JsonNode log = createLog(auth, LocalDate.now(), "历史日志");
        update(user.path("id").asLong(), Map.of("disabled", true));
        mvc.perform(get("/api/v1/logs").header("Authorization", auth)).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("username", username, "password", "demo1234"))))
                .andExpect(status().isUnauthorized());
        assertThat(logs.findById(log.path("id").asLong())).isPresent();
        update(user.path("id").asLong(), Map.of("disabled", false));
        mvc.perform(get("/api/v1/logs").header("Authorization", bearer(username)))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].content").value("历史日志"));
    }

    @Test
    void roleChangeAppliesToPreviouslyIssuedToken() throws Exception {
        JsonNode user = createUser("TEAM_LEAD", Map.of());
        JsonNode child = createUser("STAFF", Map.of("managerId", user.path("id").asLong()));
        String auth = bearer(user.path("username").asText());
        update(user.path("id").asLong(), Map.of("role", "STAFF"));
        mvc.perform(get("/api/v1/logs").param("userId", child.path("id").asText())
                        .header("Authorization", auth)).andExpect(status().isForbidden());
        JsonNode detached = createUser("STAFF", Map.of());
        String detachedAuth = bearer(detached.path("username").asText());
        update(detached.path("id").asLong(), Map.of("role", "ADMIN"));
        mvc.perform(get("/api/v1/admin/users").header("Authorization", detachedAuth)).andExpect(status().isOk());
        mvc.perform(get("/api/v1/logs").header("Authorization", detachedAuth)).andExpect(status().isForbidden());
    }

    @Test
    void normalizedDuplicateAndInvalidUserInputReturn400() throws Exception {
        String auth = bearer("admin");
        mvc.perform(post("/api/v1/admin/users").header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("username", " staff ", "password", "demo1234", "role", "STAFF"))))
                .andExpect(status().isBadRequest());
        for (String payload : new String[] {"{}", "{\"username\":\"abc\",\"password\":\"demo1234\",\"role\":\"LEADER\"}",
                "{\"username\":\"  a  \",\"password\":\"demo1234\",\"role\":\"STAFF\"}"}) {
            mvc.perform(post("/api/v1/admin/users").header("Authorization", auth)
                            .contentType(MediaType.APPLICATION_JSON).content(payload)).andExpect(status().isBadRequest());
        }
    }

    @Test
    void invalidOrganizationUpdatesAreAtomicAndCanBeCleared() throws Exception {
        JsonNode lead = createUser("TEAM_LEAD", Map.of("displayName", "原名称"));
        long leadId = lead.path("id").asLong();
        JsonNode staff = createUser("STAFF", Map.of("managerId", leadId));
        String auth = bearer("admin");
        for (long manager : new long[] {leadId, staff.path("id").asLong(), Long.MAX_VALUE}) {
            mvc.perform(patch("/api/v1/admin/users/" + leadId).header("Authorization", auth)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(Map.of("role", "STAFF", "displayName", "不能保存", "disabled", true, "managerId", manager))))
                    .andExpect(status().isBadRequest());
            var stored = users.findById(leadId).orElseThrow();
            assertThat(stored.getRole()).isEqualTo(Role.TEAM_LEAD);
            assertThat(stored.getDisplayName()).isEqualTo("原名称");
            assertThat(stored.getManagerId()).isNull();
            assertThat(stored.isDisabled()).isFalse();
        }
        mvc.perform(patch("/api/v1/admin/users/" + leadId).header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"role\":\"ADMIN\"}"))
                .andExpect(status().isBadRequest());
        assertThat(users.findById(leadId).orElseThrow().getRole()).isEqualTo(Role.TEAM_LEAD);
        mvc.perform(patch("/api/v1/admin/users/" + staff.path("id").asLong()).header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"role\":\"ADMIN\"}"))
                .andExpect(status().isBadRequest());
        update(staff.path("id").asLong(), Map.of("clearManager", true));
        assertThat(users.findById(staff.path("id").asLong()).orElseThrow().getManagerId()).isNull();
        update(leadId, Map.of("role", "ADMIN"));
    }

    @Test
    void adminCannotBeAttachedToBusinessOrganization() throws Exception {
        long leadId = users.findByUsername("team_lead").orElseThrow().getId();
        mvc.perform(post("/api/v1/admin/users").header("Authorization", bearer("admin"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("username", uniqueName(), "password", "demo1234", "role", "ADMIN", "managerId", leadId))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void administratorCreatesACompleteAppOrganizationAndLogFlow() throws Exception {
        JsonNode founder = createUser("FOUNDER", Map.of());
        JsonNode department = createUser("DEPT_HEAD", Map.of("managerId", founder.path("id").asLong()));
        JsonNode lead = createUser("TEAM_LEAD", Map.of("managerId", department.path("id").asLong()));
        JsonNode staff = createUser("STAFF", Map.of("managerId", lead.path("id").asLong()));
        JsonNode saved = createLog(bearer(staff.path("username").asText()), LocalDate.now(), "晨星冒烟测试校验通过");
        for (JsonNode superior : new JsonNode[] {founder, department, lead}) {
            mvc.perform(get("/api/v1/logs").param("userId", staff.path("id").asText())
                            .header("Authorization", bearer(superior.path("username").asText())))
                    .andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(saved.path("id").asInt()));
        }
        mvc.perform(patch("/api/v1/admin/users/" + staff.path("id").asLong())
                        .header("Authorization", bearer("admin"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"disabled\":true}"))
                .andExpect(status().isOk());
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("username", staff.path("username").asText(), "password", "demo1234"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void integrityDoesNotHideStorageFailure() {
        WorkLogRepository repository = mock(WorkLogRepository.class);
        AccessService access = mock(AccessService.class);
        when(access.currentUser()).thenReturn(new UserPrincipal(1L, "staff", Role.STAFF, null));
        when(repository.findByUserIdAndLogDateOrderByCreatedAtDesc(eq(1L), any(LocalDate.class)))
                .thenThrow(new IllegalStateException("database unavailable"));
        assertThatThrownBy(() -> new WorkLogController(repository, access).integrity())
                .isInstanceOf(IllegalStateException.class);
    }

    private JsonNode createUser(String role, Map<String, Object> fields) throws Exception {
        var payload = new java.util.HashMap<String, Object>(fields);
        payload.put("username", uniqueName());
        payload.put("password", "demo1234");
        payload.put("role", role);
        return read(mvc.perform(post("/api/v1/admin/users").header("Authorization", bearer("admin"))
                        .contentType(MediaType.APPLICATION_JSON).content(json(payload)))
                .andExpect(status().isCreated()).andReturn());
    }

    private void update(long id, Map<String, Object> fields) throws Exception {
        mvc.perform(patch("/api/v1/admin/users/" + id).header("Authorization", bearer("admin"))
                        .contentType(MediaType.APPLICATION_JSON).content(json(fields)))
                .andExpect(status().isOk());
    }

    private JsonNode createLog(String auth, LocalDate day, String text) throws Exception {
        return read(mvc.perform(post("/api/v1/logs").header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("logDate", day.toString(), "content", text))))
                .andExpect(status().isCreated()).andReturn());
    }

    private JsonNode login(String username) throws Exception {
        return read(mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("username", username, "password", "demo1234"))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.token").isNotEmpty()).andReturn());
    }

    private String bearer(String username) throws Exception {
        return "Bearer " + login(username).path("token").asText();
    }

    private String json(Object value) throws Exception {
        return mapper.writeValueAsString(value);
    }

    private JsonNode read(MvcResult result) throws Exception {
        return mapper.readTree(result.getResponse().getContentAsByteArray());
    }

    private String uniqueName() {
        return "s1_" + UUID.randomUUID().toString().replace("-", "");
    }
}
