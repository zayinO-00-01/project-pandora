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
class NotificationTests {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired com.projectpandora.api.task.TaskRepository tasks;

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
    void progress(JsonNode user, long id, String status, Integer value, String note) throws Exception {
        var body = new HashMap<String, Object>();
        body.put("status", status); body.put("progressNote", note);
        if (value != null) body.put("progress", value);
        mvc.perform(put("/api/v1/tasks/" + id + "/progress").header("Authorization", token(user))
            .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)))
            .andExpect(status().isOk());
    }
    void review(JsonNode user, long id, String action, String note) throws Exception {
        mvc.perform(post("/api/v1/tasks/" + id + "/" + action).header("Authorization", token(user))
            .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("note", note == null ? "" : note))))
            .andExpect(status().isOk());
    }
    JsonNode notifications(JsonNode user) throws Exception {
        return json.readTree(mvc.perform(get("/api/v1/notifications").header("Authorization", token(user)))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8));
    }
    boolean hasType(JsonNode list, String type) {
        for (JsonNode n : list.get("items")) if (type.equals(n.get("type").asText())) return true;
        return false;
    }

    @Test void dispatchNotifiesAssignee() throws Exception {
        var team = login("team_lead"); var staff = login("staff");
        create(team, staff.get("userId").asLong(), "派发通知任务");
        var list = notifications(staff);
        assertThat(hasType(list, "task_assigned")).isTrue();
        assertThat(list.get("unreadCount").asLong()).isGreaterThanOrEqualTo(1);
    }

    @Test void completionNotifiesCurrentManagerThenGenericDecisionNotifiesApplicant() throws Exception {
        var team = login("team_lead"); var staff = login("staff");
        long id = create(team, staff.get("userId").asLong(), "验收通知任务").get("id").asLong();
        progress(staff, id, "done", 100, "完成");
        assertThat(hasType(notifications(team), "task-review")).isTrue();
        assertThat(notifications(team).toString()).contains("任务待审核");
        TaskPolicyTestSupport.decide(mvc,json,id,"team_lead","approved");
        assertThat(notifications(staff).toString()).contains("任务审核已批准");
    }

    @Test void genericRejectionNotifiesApplicantAndPreservesEffectiveState() throws Exception {
        var team = login("team_lead"); var staff = login("staff");
        long id = create(team, staff.get("userId").asLong(), "驳回通知任务").get("id").asLong();
        progress(staff, id, "done", null, "完成");
        TaskPolicyTestSupport.decide(mvc,json,id,"team_lead","rejected");
        var list = notifications(staff);
        assertThat(hasType(list, "task-review")).isTrue();
        assertThat(list.toString()).contains("任务审核已拒绝");
        var task=TaskPolicyTestSupport.task(mvc,json,id,"staff");
        assertThat(task.get("status").asText()).isEqualTo("todo");assertThat(task.get("progressNote").asText()).isEmpty();
    }

    @Test void legacyCompletionRejectionKeepsReasonNotification() throws Exception {
        var founder=login("founder");var team=login("team_lead");var staff=login("staff");
        long id=create(founder,staff.get("userId").asLong(),"历史验收通知").get("id").asLong();
        var task=tasks.findById(id).orElseThrow();
        org.springframework.test.util.ReflectionTestUtils.setField(task,"status","done");
        task.setReviewStatus("pending_done");tasks.saveAndFlush(task);
        review(team,id,"reject","请补充历史验收依据");
        var list=notifications(staff);
        assertThat(hasType(list,"done_rejected")).isTrue();
        assertThat(list.toString()).contains("请补充历史验收依据");
        assertThat(TaskPolicyTestSupport.task(mvc,json,id,"staff").get("status").asText()).isEqualTo("doing");
    }

    @Test void changeAndDeleteGoToSuperior() throws Exception {
        var team = login("team_lead"); var staff = login("staff"); var leader = login("leader");
        long id = create(team, staff.get("userId").asLong(), "变更通知任务").get("id").asLong();
        var change = new HashMap<String, Object>();
        change.put("dueAt", "2026-11-01T10:00:00Z"); change.put("title", "改标题"); change.put("assigneeId", staff.get("userId").asLong());
        mvc.perform(put("/api/v1/tasks/" + id).header("Authorization", token(team))
            .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(change))).andExpect(status().isOk());
        assertThat(hasType(notifications(leader), "change_pending")).isTrue();
        review(leader, id, "approve", "同意");
        assertThat(hasType(notifications(team), "change_approved")).isTrue();
    }

    @Test void markAllReadClearsUnread() throws Exception {
        var team = login("team_lead"); var staff = login("staff");
        create(team, staff.get("userId").asLong(), "已读任务");
        assertThat(notifications(staff).get("unreadCount").asLong()).isGreaterThanOrEqualTo(1);
        mvc.perform(post("/api/v1/notifications/read-all").header("Authorization", token(staff)))
            .andExpect(status().isNoContent());
        assertThat(notifications(staff).get("unreadCount").asLong()).isZero();
    }
}
