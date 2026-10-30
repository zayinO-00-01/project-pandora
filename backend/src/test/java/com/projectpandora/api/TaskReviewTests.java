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
    @Autowired com.projectpandora.api.task.TaskRepository tasks;
    @Autowired com.projectpandora.api.user.UserRepository users;

    JsonNode login(String name) throws Exception {
        return json.readTree(mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsString(Map.of("username", name, "password", "demo1234"))))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8));
    }
    String token(JsonNode user) { return "Bearer " + user.get("token").asText(); }

    JsonNode create(JsonNode creator, long assigneeId, String title) throws Exception {
        var body = new HashMap<String, Object>();
        body.put("dueAt", "2026-11-01T10:00:00Z"); body.put("title", title); body.put("assigneeId", assigneeId);
        var created=json.readTree(mvc.perform(post("/api/v1/tasks").header("Authorization", token(creator))
            .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)))
            .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8));
        return TaskPolicyTestSupport.approveInitial(mvc,json,created,"leader");
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

    @Test void progressIsReviewedByCurrentAssigneeManagerAndCompletedTaskCanReopen() throws Exception {
        var team=login("team_lead");var staff=login("staff");var leader=login("leader");
        long id=create(team,staff.get("userId").asLong(),"验收任务").get("id").asLong();
        var unchanged=progress(staff,id,"done",100,"完成");
        assertThat(unchanged.get("status").asText()).isEqualTo("todo");
        assertThat(unchanged.get("reviewStatus").asText()).isEqualTo("none");
        assertThat(reviews(team).isEmpty()).isTrue();
        long review=TaskPolicyTestSupport.pendingReview(mvc,json,id,"team_lead");
        mvc.perform(put("/api/v1/task-reviews/"+review).header("Authorization",token(leader)).contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"approved\"}")).andExpect(status().isForbidden());
        TaskPolicyTestSupport.decide(mvc,json,id,"team_lead","approved");
        assertThat(TaskPolicyTestSupport.task(mvc,json,id,"staff").get("status").asText()).isEqualTo("done");
        assertThat(progress(staff,id,"doing",40,"重新处理").get("status").asText()).isEqualTo("done");
        TaskPolicyTestSupport.decide(mvc,json,id,"team_lead","approved");
        assertThat(TaskPolicyTestSupport.task(mvc,json,id,"staff").get("status").asText()).isEqualTo("doing");
    }

    @Test void rejectedCompletionPreservesEffectiveFeedbackAndAllowsResubmission() throws Exception {
        var team=login("team_lead");var staff=login("staff");
        long id=create(team,staff.get("userId").asLong(),"驳回任务").get("id").asLong();
        progress(staff,id,"doing",40,"先前进度");TaskPolicyTestSupport.decide(mvc,json,id,"team_lead","approved");
        var original=TaskPolicyTestSupport.task(mvc,json,id,"staff");
        assertThat(progress(staff,id,"done",null,"完成")).isEqualTo(original);
        TaskPolicyTestSupport.decide(mvc,json,id,"team_lead","rejected");
        assertThat(TaskPolicyTestSupport.task(mvc,json,id,"staff")).isEqualTo(original);
        progress(staff,id,"doing",60,"继续处理");TaskPolicyTestSupport.decide(mvc,json,id,"team_lead","approved");
        assertThat(TaskPolicyTestSupport.task(mvc,json,id,"staff").get("progress").asInt()).isEqualTo(60);
    }

    @Test void legacyCompletionReviewUsesCurrentAssigneeManagerWithoutFounderOverride() throws Exception {
        var founder=login("founder");var staff=login("staff");var team=login("team_lead");var leader=login("leader");
        long id=create(founder,staff.get("userId").asLong(),"历史验收").get("id").asLong();
        var task=tasks.findById(id).orElseThrow();org.springframework.test.util.ReflectionTestUtils.setField(task,"status","done");task.setReviewStatus("pending_done");tasks.saveAndFlush(task);
        var assignee=users.findByUsername("staff").orElseThrow();assignee.setManagerId(leader.get("userId").asLong());users.saveAndFlush(assignee);
        assertThat(reviews(team).isEmpty()).isTrue();assertThat(reviews(leader).findValuesAsText("id")).contains(""+id);
        review(team,id,"approve","",403);review(founder,id,"approve","",403);review(leader,id,"approve","历史验收通过",200);
        assertThat(TaskPolicyTestSupport.task(mvc,json,id,"staff").get("reviewStatus").asText()).isEqualTo("approved");
        assertThat(TaskPolicyTestSupport.task(mvc,json,id,"staff").get("status").asText()).isEqualTo("done");
    }

    @Test void definitionChangeIsReviewedBySuperiorAndAppliedOnApproval() throws Exception {
        var team = login("team_lead"); var staff = login("staff"); var leader = login("leader");
        long id = create(team, staff.get("userId").asLong(), "原标题").get("id").asLong();
        var change = new HashMap<String, Object>();
        change.put("dueAt", "2026-11-01T10:00:00Z"); change.put("title", "新标题"); change.put("assigneeId", staff.get("userId").asLong());
        mvc.perform(put("/api/v1/tasks/" + id).header("Authorization", token(team))
            .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(change))).andExpect(status().isOk());
        assertThat(reviews(team).findValuesAsText("id")).doesNotContain("" + id);
        assertThat(reviews(leader).findValuesAsText("id")).contains("" + id);
        mvc.perform(put("/api/v1/tasks/"+id+"/progress").header("Authorization",token(staff)).contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"doing\",\"progressNote\":\"先干着\"}")).andExpect(status().isConflict());
        review(leader, id, "approve", "同意", 200);
        progress(staff,id,"doing",30,"获批后执行");TaskPolicyTestSupport.decide(mvc,json,id,"team_lead","approved");
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
        TaskPolicyTestSupport.decide(mvc,json,progressed,"team_lead","approved");
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
        var generic=json.readTree(mvc.perform(get("/api/v1/task-reviews").header("Authorization",token(staff))).andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        assertThat(generic.get(0).get("status").asText()).isEqualTo("canceled");
        assertThat(events.findByTaskIdOrderByIdAsc(id).stream().map(e -> e.getEventType()).toList()).contains("deleted");
    }
}
