package com.projectpandora.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.projectpandora.api.task.TaskEventRepository;
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
class TaskReviewTests {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired TaskEventRepository events;

    JsonNode login(String name) throws Exception {
        return json.readTree(mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsString(Map.of("username", name, "password", "demo1234"))))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8));
    }
    String token(JsonNode user) { return "Bearer " + user.get("token").asText(); }

    JsonNode create(JsonNode creator, long assigneeId, String title) throws Exception {
        var body = new HashMap<String, Object>();
        body.put("title", title); body.put("assigneeId", assigneeId);
        return json.readTree(mvc.perform(post("/api/v1/tasks").header("Authorization", token(creator))
            .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)))
            .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8));
    }
    JsonNode progress(JsonNode user, long id, String status, Integer value, String note) throws Exception {
        var body = new HashMap<String, Object>();
        body.put("status", status); body.put("progressNote", note);
        if (value != null) body.put("progress", value);
        return json.readTree(mvc.perform(put("/api/v1/tasks/" + id + "/progress").header("Authorization", token(user))
            .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8));
    }
    JsonNode reviews(JsonNode user) throws Exception {
        return json.readTree(mvc.perform(get("/api/v1/tasks/reviews").header("Authorization", token(user)))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8));
    }
    void review(JsonNode user, long id, String action, String note, int expected) throws Exception {
        mvc.perform(post("/api/v1/tasks/" + id + "/" + action).header("Authorization", token(user))
            .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("note", note == null ? "" : note))))
            .andExpect(status().is(expected));
    }

    @Test void completionIsReviewedByDispatcherAndNotByAnyoneElse() throws Exception {
        var team = login("team_lead"); var staff = login("staff"); var leader = login("leader");
        long id = create(team, staff.get("userId").asLong(), "验收任务").get("id").asLong();
        var done = progress(staff, id, "done", 100, "完成");
        assertThat(done.get("reviewStatus").asText()).isEqualTo("pending_done");
        assertThat(reviews(team).findValuesAsText("id")).contains("" + id);
        assertThat(reviews(leader).findValuesAsText("id")).doesNotContain("" + id);
        review(leader, id, "approve", "", 403);
        review(team, id, "approve", "通过", 200);
        assertThat(reviews(team).findValuesAsText("id")).doesNotContain("" + id);
        mvc.perform(put("/api/v1/tasks/" + id + "/progress").header("Authorization", token(staff))
            .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("status", "doing", "progressNote", "改"))))
            .andExpect(status().isForbidden());
    }

    @Test void rejectedCompletionReturnsToDoingAndClearsOnNextProgress() throws Exception {
        var team = login("team_lead"); var staff = login("staff");
        long id = create(team, staff.get("userId").asLong(), "驳回任务").get("id").asLong();
        progress(staff, id, "done", null, "完成");
        review(team, id, "reject", "请补充说明", 200);
        assertThat(reviews(team).findValuesAsText("id")).doesNotContain("" + id);
        var resumed = progress(staff, id, "doing", 40, "继续处理");
        assertThat(resumed.get("status").asText()).isEqualTo("doing");
        assertThat(resumed.get("reviewStatus").asText()).isEqualTo("none");
    }

    @Test void definitionChangeIsReviewedBySuperiorAndAppliedOnApproval() throws Exception {
        var team = login("team_lead"); var staff = login("staff"); var leader = login("leader");
        long id = create(team, staff.get("userId").asLong(), "原标题").get("id").asLong();
        var change = new HashMap<String, Object>();
        change.put("title", "新标题"); change.put("assigneeId", staff.get("userId").asLong());
        mvc.perform(put("/api/v1/tasks/" + id).header("Authorization", token(team))
            .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(change))).andExpect(status().isOk());
        assertThat(reviews(team).findValuesAsText("id")).doesNotContain("" + id);
        assertThat(reviews(leader).findValuesAsText("id")).contains("" + id);
        // Not frozen: assignee can still move progress while the change awaits review.
        assertThat(progress(staff, id, "doing", 30, "先干着").get("status").asText()).isEqualTo("doing");
        review(leader, id, "approve", "同意", 200);
        var list = json.readTree(mvc.perform(get("/api/v1/tasks").header("Authorization", token(team)))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8));
        assertThat(list.findValuesAsText("title")).contains("新标题");
    }

    @Test void untouchedTaskDeletesDirectlyButProgressTaskNeedsReview() throws Exception {
        var team = login("team_lead"); var staff = login("staff"); var leader = login("leader");
        long plain = create(team, staff.get("userId").asLong(), "待删任务").get("id").asLong();
        mvc.perform(delete("/api/v1/tasks/" + plain).header("Authorization", token(team))).andExpect(status().isNoContent());
        var afterPlain = json.readTree(mvc.perform(get("/api/v1/tasks").header("Authorization", token(team)))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8));
        assertThat(afterPlain.findValuesAsText("id")).doesNotContain("" + plain);

        long progressed = create(team, staff.get("userId").asLong(), "有进度任务").get("id").asLong();
        progress(staff, progressed, "doing", 50, "进行中");
        mvc.perform(delete("/api/v1/tasks/" + progressed).header("Authorization", token(team))).andExpect(status().isNoContent());
        assertThat(reviews(leader).findValuesAsText("id")).contains("" + progressed);
        review(leader, progressed, "approve", "同意删除", 200);
        var afterProgressed = json.readTree(mvc.perform(get("/api/v1/tasks").header("Authorization", token(staff)))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8));
        assertThat(afterProgressed.findValuesAsText("id")).doesNotContain("" + progressed);
    }

    @Test void founderDeletesAnyTaskAndEventsAreRecorded() throws Exception {
        var team = login("team_lead"); var staff = login("staff"); var founder = login("founder");
        long id = create(team, staff.get("userId").asLong(), "创始人删除").get("id").asLong();
        progress(staff, id, "done", 100, "完成");
        var types = events.findByTaskIdOrderByIdAsc(id).stream().map(e -> e.getEventType()).toList();
        assertThat(types).contains("created", "review_requested");
        assertThat(events.findByTaskIdOrderByIdAsc(id)).isNotEmpty();
        mvc.perform(delete("/api/v1/tasks/" + id).header("Authorization", token(founder))).andExpect(status().isNoContent());
        var left = json.readTree(mvc.perform(get("/api/v1/tasks").header("Authorization", token(team)))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8));
        assertThat(left.findValuesAsText("id")).doesNotContain("" + id);
        assertThat(events.findByTaskIdOrderByIdAsc(id).stream().map(e -> e.getEventType()).toList()).contains("deleted");
    }
}
