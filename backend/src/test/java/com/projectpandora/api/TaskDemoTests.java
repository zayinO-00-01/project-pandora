package com.projectpandora.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class TaskDemoTests {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;

    JsonNode login(String name) throws Exception {
        return json.readTree(mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsString(Map.of("username", name, "password", "demo1234"))))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8));
    }
    JsonNode unrelated() throws Exception {
        return json.readTree(mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsString(Map.of("username", "task_unrelated", "password", "demo1234", "displayName", "外部成员"))))
            .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8));
    }
    String token(JsonNode user) { return "Bearer " + user.get("token").asText(); }
    String payload(JsonNode assignee, String title) throws Exception {
        return json.writeValueAsString(Map.of("title", title, "detail", "任务详情", "priority", "high",
            "assigneeId", assignee.get("userId").asLong(), "dueAt", "2026-10-01T15:59:59Z"));
    }
    JsonNode create(JsonNode creator, JsonNode assignee, String title) throws Exception {
        return json.readTree(mvc.perform(post("/api/v1/tasks").header("Authorization", token(creator))
            .contentType(MediaType.APPLICATION_JSON).content(payload(assignee, title)))
            .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8));
    }
    JsonNode list(JsonNode user) throws Exception {
        return json.readTree(mvc.perform(get("/api/v1/tasks").header("Authorization", token(user)))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8));
    }
    JsonNode progress(JsonNode user, long id, int value, String note) throws Exception {
        return json.readTree(mvc.perform(put("/api/v1/tasks/" + id + "/progress").header("Authorization", token(user))
            .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("progress", value, "progressNote", note))))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8));
    }

    @Test void dispatchFeedbackRoundTripKeepsIdentityAndImmutableFields() throws Exception {
        var leader = login("leader"); var staff = login("staff");
        var task = create(leader, staff, "  设备检查  "); long id = task.get("id").asLong();
        assertThat(task.get("title").asText()).isEqualTo("设备检查");
        assertThat(task.get("status").asText()).isEqualTo("todo");
        assertThat(task.get("progress").asInt()).isZero();
        assertThat(task.get("progressNote").asText()).isEmpty();
        assertThat(task.get("createdBy").asLong()).isEqualTo(leader.get("userId").asLong());
        assertThat(task.get("assigneeId").asLong()).isEqualTo(staff.get("userId").asLong());
        assertThat(task.get("creatorName").asText()).isNotBlank();
        assertThat(task.get("assigneeName").asText()).isNotBlank();
        assertThat(task.get("createdAt").asText()).isNotBlank();
        assertThat(task.get("updatedAt").asText()).isNotBlank();
        assertThat(list(staff).get(0).get("id").asLong()).isEqualTo(id);
        var doing = progress(staff, id, 50, "已检查一半");
        assertThat(doing.get("status").asText()).isEqualTo("doing");
        assertThat(list(leader).get(0).get("progressNote").asText()).isEqualTo("已检查一半");
        var done = progress(staff, id, 100, "检查完成");
        assertThat(done.get("status").asText()).isEqualTo("done");
        assertThat(list(leader).get(0).get("progress").asInt()).isEqualTo(100);
        for (String field : new String[]{"id", "title", "detail", "priority", "dueAt", "createdBy", "assigneeId", "createdAt"})
            assertThat(done.get(field)).as(field).isEqualTo(task.get(field));
        assertThat(progress(staff, id, 0, "重新检查").get("status").asText()).isEqualTo("todo");
    }

    @Test void roleScopesExcludeUnrelatedAssignments() throws Exception {
        var admin = login("admin"); var leader = login("leader"); var staff = login("staff"); var other = unrelated();
        var direct = create(admin, staff, "直属任务");
        var own = create(admin, leader, "领导本人任务");
        var hidden = create(admin, other, "无关任务");
        var created = create(leader, staff, "领导派发任务");
        assertThat(list(admin).size()).isEqualTo(4);
        assertThat(list(leader).findValuesAsText("id")).containsExactly(created.get("id").asText(), own.get("id").asText(), direct.get("id").asText());
        assertThat(list(staff).findValuesAsText("id")).containsExactly(created.get("id").asText(), direct.get("id").asText());
        assertThat(list(other).findValuesAsText("id")).containsExactly(hidden.get("id").asText());
        mvc.perform(post("/api/v1/tasks").header("Authorization", token(leader)).contentType(MediaType.APPLICATION_JSON).content(payload(other, "越权")))
            .andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/tasks").header("Authorization", token(leader)).contentType(MediaType.APPLICATION_JSON).content(payload(leader, "自派")))
            .andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/tasks").header("Authorization", token(staff)).contentType(MediaType.APPLICATION_JSON).content(payload(staff, "越权")))
            .andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/tasks").header("Authorization", token(admin)).contentType(MediaType.APPLICATION_JSON).content(payload(admin, "管理员")))
            .andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/tasks").header("Authorization", token(admin)).contentType(MediaType.APPLICATION_JSON)
            .content("{\"title\":\"不存在\",\"assigneeId\":9223372036854775807}"))
            .andExpect(status().isNotFound());
        assertThat(list(admin).size()).isEqualTo(4);
    }

    @Test void onlyAssigneeCanUpdateIncludingCreatorAndAdmin() throws Exception {
        var leader = login("leader"); var staff = login("staff"); var admin = login("admin"); var other = unrelated();
        var task = create(leader, staff, "受保护任务"); long id = task.get("id").asLong();
        for (var user : new JsonNode[]{leader, admin, other})
            mvc.perform(put("/api/v1/tasks/" + id + "/progress").header("Authorization", token(user))
                .contentType(MediaType.APPLICATION_JSON).content("{\"progress\":100,\"progressNote\":\"越权完成\"}"))
                .andExpect(status().isForbidden());
        assertThat(list(staff).get(0)).isEqualTo(task);
        mvc.perform(put("/api/v1/tasks/9223372036854775807/progress").header("Authorization", token(staff))
            .contentType(MediaType.APPLICATION_JSON).content("{\"progress\":50,\"progressNote\":\"不存在\"}"))
            .andExpect(status().isNotFound());
    }

    @Test void invalidProgressDoesNotMutateStoredTask() throws Exception {
        var leader = login("leader"); var staff = login("staff"); var task = create(leader, staff, "校验任务");
        long id = task.get("id").asLong(); var original = progress(staff, id, 50, "原有进度");
        String[] invalid = {"{bad", "{}", "{\"progress\":null,\"progressNote\":\"说明\"}",
            "{\"progress\":-1,\"progressNote\":\"说明\"}", "{\"progress\":101,\"progressNote\":\"说明\"}",
            "{\"progress\":50.5,\"progressNote\":\"说明\"}", "{\"progress\":\"50\",\"progressNote\":\"说明\"}",
            "{\"progress\":50,\"progressNote\":\"   \"}", "{\"progress\":50,\"progressNote\":null}",
            json.writeValueAsString(Map.of("progress", 100, "progressNote", "x".repeat(2001)))};
        for (String body : invalid) {
            mvc.perform(put("/api/v1/tasks/" + id + "/progress").header("Authorization", token(staff))
                .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isBadRequest());
            assertThat(list(staff).get(0)).as(body).isEqualTo(original);
        }
    }

    @Test void invalidCreationCreatesNothingAndDefaultsAreStable() throws Exception {
        var leader = login("leader"); var staff = login("staff");
        var defaults = new HashMap<String, Object>(); defaults.put("title", " 简单任务 "); defaults.put("assigneeId", staff.get("userId").asLong());
        defaults.put("dueAt", null);
        var task = json.readTree(mvc.perform(post("/api/v1/tasks").header("Authorization", token(leader))
            .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(defaults)))
            .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8));
        assertThat(task.get("priority").asText()).isEqualTo("normal");
        assertThat(task.get("dueAt").isNull()).isTrue();
        assertThat(task.get("detail").asText()).isEmpty();
        for (Map<String, String> change : java.util.List.of(Map.of("title", "  "), Map.of("title", "x".repeat(129)),
                Map.of("detail", "x".repeat(5001)), Map.of("priority", "urgent"), Map.of("dueAt", "not-a-date"))) {
            var body = new HashMap<>(defaults); body.putAll(change);
            mvc.perform(post("/api/v1/tasks").header("Authorization", token(leader)).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(body))).andExpect(status().isBadRequest());
        }
        for (String body : new String[]{"{bad", "{}", "{\"title\":\"无责任人\"}"})
            mvc.perform(post("/api/v1/tasks").header("Authorization", token(leader)).contentType(MediaType.APPLICATION_JSON)
                .content(body)).andExpect(status().isBadRequest());
        assertThat(list(leader).size()).isEqualTo(1);
    }

    @Test void dispatchPanelContainsOnlyTenNewestAuthorizedTasks() throws Exception {
        var leader = login("leader"); var staff = login("staff"); var admin = login("admin"); var other = unrelated();
        var ids = new java.util.ArrayList<Long>();
        for (int i = 0; i < 12; i++) ids.add(create(leader, staff, "任务" + i).get("id").asLong());
        create(admin, other, "不应显示");
        var panel = json.readTree(mvc.perform(get("/api/v1/panels/map").header("Authorization", token(staff)))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8));
        var dispatch = panel.get("companyDispatch"); assertThat(dispatch.size()).isEqualTo(10);
        for (int i = 0; i < 10; i++) {
            assertThat(dispatch.get(i).get("id").asLong()).isEqualTo(ids.get(11 - i));
            assertThat(dispatch.get(i).get("title").asText()).isEqualTo("任务" + (11 - i));
            assertThat(dispatch.get(i).get("sortOrder").asInt()).isEqualTo(i);
            assertThat(dispatch.get(i).get("myRemark").isNull()).isTrue();
        }
        assertThat(panel.get("companyImportant").isEmpty()).isTrue();
        assertThat(panel.get("personalTop").isEmpty()).isTrue();
        assertThat(panel.get("todayLogs").isEmpty()).isTrue();
        mvc.perform(get("/api/v1/panels/map").header("Authorization", token(other)))
            .andExpect(jsonPath("$.companyDispatch.length()").value(1))
            .andExpect(jsonPath("$.companyDispatch[0].title").value("不应显示"));
    }

    @Test void taskEndpointsRequireLogin() throws Exception {
        mvc.perform(get("/api/v1/tasks")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/tasks").contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isUnauthorized());
        mvc.perform(put("/api/v1/tasks/1/progress").contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isUnauthorized());
    }
}