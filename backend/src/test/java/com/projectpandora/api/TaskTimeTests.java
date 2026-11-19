package com.projectpandora.api;

import com.fasterxml.jackson.databind.*;
import com.projectpandora.api.user.UserRepository;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test") @Transactional
class TaskTimeTests {
    @Autowired org.springframework.test.web.servlet.MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired UserRepository users;
    @Autowired org.springframework.jdbc.core.JdbcTemplate jdbc;
    @Autowired jakarta.persistence.EntityManager persistence;
    String token(String name) throws Exception {return TaskPolicyTestSupport.token(mvc,json,name);}
    com.fasterxml.jackson.databind.node.ObjectNode body() {
        return json.createObjectNode().put("title","时间安排").put("assigneeId",users.findByUsername("staff").orElseThrow().getId()).put("dueAt","2090-11-03T10:00:00Z").put("startAt","2090-11-01T01:00:00Z");
    }
    JsonNode create(String actor,JsonNode body) throws Exception {
        return json.readTree(mvc.perform(post("/api/v1/tasks").header("Authorization",token(actor)).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)))
            .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8));
    }
    @Test void startAndDeadlineRequiredAndRangeValidated() throws Exception {
        String start="2090-11-01T01:00:00Z";
        assertThat(create("founder",body().put("startAt",start)).get("startAt").asText()).isEqualTo(start);
        assertThat(create("founder",body().put("startAt","2090-11-03T10:00:00Z")).get("startAt").asText()).isEqualTo("2090-11-03T10:00:00Z");
        var missing=body();missing.remove("dueAt");
        var missingStart=body();missingStart.remove("startAt");
        for(var invalid:java.util.List.of(missingStart,body().putNull("startAt"),missing,body().putNull("dueAt"),body().put("startAt","2090-11-04T01:00:00Z"),body().put("startAt","invalid")))
            mvc.perform(post("/api/v1/tasks").header("Authorization",token("founder")).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(invalid))).andExpect(status().isBadRequest());
    }
    @Test void earlierTodayIsNotAValidDeadline() throws Exception {
        var deadline=java.time.Instant.now().minusSeconds(120);
        mvc.perform(post("/api/v1/tasks").header("Authorization",token("founder")).contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsString(body().put("startAt",deadline.minusSeconds(600).toString()).put("dueAt",deadline.toString())))).andExpect(status().isBadRequest());
    }
    @Test void expiredCreationRequestCanBeRejectedButCannotBeApproved() throws Exception {
        long id=create("staff",body()).get("id").asLong();
        var now=java.time.Instant.now();
        jdbc.update("UPDATE dispatch_tasks SET start_at=?,due_at=? WHERE id=?",
            java.sql.Timestamp.from(now.minusSeconds(600)),java.sql.Timestamp.from(now.minusSeconds(120)),id);
        persistence.clear();
        var path="/api/v1/tasks/"+id+"/approval";
        var reviewer=token("team_lead");
        mvc.perform(put(path).header("Authorization",reviewer).contentType(MediaType.APPLICATION_JSON)
            .content("{\"decision\":\"approved\"}")).andExpect(status().isBadRequest());
        mvc.perform(put(path).header("Authorization",reviewer).contentType(MediaType.APPLICATION_JSON)
            .content("{\"decision\":\"rejected\"}")).andExpect(status().isOk()).andExpect(jsonPath("$.approvalStatus").value("rejected"));
    }
    @Test void assigneeMovesDeadlineAfterTheirManagersApproval() throws Exception {
        var original=create("founder",body()); long id=original.get("id").asLong();
        var deadline="2090-11-05T10:00:00Z"; var payload=json.writeValueAsString(Map.of("dueAt",deadline));
        var path="/api/v1/tasks/"+id+"/due-date";
        mvc.perform(put(path).header("Authorization",token("team_lead")).contentType(MediaType.APPLICATION_JSON).content(payload)).andExpect(status().isForbidden());
        mvc.perform(put(path).header("Authorization",token("staff")).contentType(MediaType.APPLICATION_JSON).content(payload)).andExpect(status().isOk()).andExpect(jsonPath("$.dueAt").value(original.get("dueAt").asText()));
        TaskPolicyTestSupport.decide(mvc,json,id,"team_lead","approved");
        var changed=TaskPolicyTestSupport.task(mvc,json,id,"staff");
        assertThat(changed.get("dueAt").asText()).isEqualTo(deadline);
        assertThat(changed.get("startAt")).isEqualTo(original.get("startAt"));
    }
    @Test void editingTimesWaitsForReviewAndCannotClearRequiredStart() throws Exception {
        var original=create("staff",body().put("startAt","2090-11-01T01:00:00Z"));
        original=TaskPolicyTestSupport.approveInitial(mvc,json,original,"team_lead");
        long id=original.get("id").asLong();
        var change=body().put("startAt","2090-11-02T01:00:00Z");
        mvc.perform(put("/api/v1/tasks/"+id).header("Authorization",token("staff")).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(change)))
            .andExpect(status().isOk()).andExpect(jsonPath("$.startAt").value("2090-11-01T01:00:00Z"));
        mvc.perform(post("/api/v1/tasks/"+id+"/approve").header("Authorization",token("team_lead"))).andExpect(status().isOk());
        assertThat(TaskPolicyTestSupport.task(mvc,json,id,"staff").get("startAt").asText()).isEqualTo("2090-11-02T01:00:00Z");
        mvc.perform(put("/api/v1/tasks/"+id).header("Authorization",token("founder")).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body().putNull("startAt"))))
            .andExpect(status().isBadRequest());
        assertThat(TaskPolicyTestSupport.task(mvc,json,id,"staff").get("startAt").asText()).isEqualTo("2090-11-02T01:00:00Z");
        var invalid=body();invalid.remove("dueAt");
        mvc.perform(put("/api/v1/tasks/"+id).header("Authorization",token("founder")).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(invalid))).andExpect(status().isBadRequest());
    }
    @Test void legacyTaskRequiresSupplementalStartAndCanThenBeMoved() throws Exception {
        long id=create("founder",body()).get("id").asLong();
        jdbc.update("UPDATE dispatch_tasks SET start_at=NULL WHERE id=?",id);
        persistence.clear();
        mvc.perform(put("/api/v1/tasks/"+id).header("Authorization",token("founder")).contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsString(body().put("title","调整旧任务名称").putNull("startAt"))))
            .andExpect(status().isOk()).andExpect(jsonPath("$.title").value("调整旧任务名称"));
        var path="/api/v1/tasks/"+id+"/due-date";
        var newDue="2090-11-05T10:00:00Z";
        mvc.perform(put(path).header("Authorization",token("founder")).contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsString(Map.of("dueAt",newDue)))).andExpect(status().isBadRequest());
        mvc.perform(put(path).header("Authorization",token("founder")).contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsString(Map.of("dueAt",newDue,"startAt","2090-11-02T10:00:00Z"))))
            .andExpect(status().isOk()).andExpect(jsonPath("$.dueAt").value(newDue))
            .andExpect(jsonPath("$.startAt").value("2090-11-02T10:00:00Z"));
    }
    @Test void expiredDeadlineAllowsTextEditButNotChangeToAnotherExpiredTime() throws Exception {
        long id=create("founder",body()).get("id").asLong();
        var start="2020-11-01T01:00:00Z";
        var due="2020-11-03T10:00:25Z";
        jdbc.update("UPDATE dispatch_tasks SET start_at=?,due_at=? WHERE id=?",
            java.sql.Timestamp.from(java.time.Instant.parse(start)),java.sql.Timestamp.from(java.time.Instant.parse(due)),id);
        persistence.clear();
        var edited=body().put("title","补充任务说明").put("startAt",start).put("dueAt",due);
        var path="/api/v1/tasks/"+id;
        mvc.perform(put(path).header("Authorization",token("founder")).contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsString(edited))).andExpect(status().isOk())
            .andExpect(jsonPath("$.title").value("补充任务说明")).andExpect(jsonPath("$.dueAt").value(due));
        mvc.perform(put(path).header("Authorization",token("founder")).contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsString(edited.put("dueAt","2020-11-04T10:00:00Z")))).andExpect(status().isBadRequest());
    }
}
