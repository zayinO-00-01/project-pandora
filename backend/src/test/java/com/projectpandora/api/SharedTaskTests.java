package com.projectpandora.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test") @Transactional
class SharedTaskTests {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired JdbcTemplate db;

    @Test void secondaryAssigneeSharesOneTaskAndUsesTheirOwnManagerForUpdates() throws Exception {
        long before = taskCount();
        long primary = userId("staff");
        long secondary = userId("team_lead");
        var original = create("founder", input(primary, secondary));
        long id = original.path("id").asLong();
        String path = "/api/v1/tasks/" + id;
        assertThat(taskCount()).isEqualTo(before + 1);
        assertThat(original.path("assigneeId").asLong()).isEqualTo(primary);
        assertThat(original.path("assigneeIds")).extracting(JsonNode::asLong).containsExactly(primary,secondary);
        assertThat(db.queryForObject("select count(*) from notifications where task_id=? and type='task_assigned'",Long.class,id)).isEqualTo(2);
        TaskPolicyTestSupport.task(mvc,json,id,"team_lead");
        mvc.perform(get("/api/v1/tasks").param("userId",String.valueOf(secondary)).header("Authorization",token("leader")))
            .andExpect(status().isOk()).andExpect(jsonPath("$[?(@.id == " + id + ")]").isNotEmpty());
        mvc.perform(get(path).header("Authorization",token("staff"))).andExpect(status().isOk());
        mvc.perform(put(path+"/note").header("Authorization",token("team_lead")).contentType(MediaType.APPLICATION_JSON)
            .content("{\"content\":\"private\"}")).andExpect(status().isOk());
        mvc.perform(get(path+"/note").header("Authorization",token("staff"))).andExpect(status().isOk())
            .andExpect(jsonPath("$.content").value(""));

        mvc.perform(put(path+"/progress").header("Authorization",token("team_lead")).contentType(MediaType.APPLICATION_JSON)
            .content("{\"status\":\"done\",\"progressNote\":\"shared\"}")).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("todo"));
        TaskPolicyTestSupport.decide(mvc,json,id,"leader","approved");
        mvc.perform(get(path).header("Authorization",token("staff"))).andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("done")).andExpect(jsonPath("$.progressNote").value("shared"));

        var deadline = Instant.now().plusSeconds(172800).truncatedTo(java.time.temporal.ChronoUnit.SECONDS).toString();
        mvc.perform(put(path+"/due-date").header("Authorization",token("team_lead")).contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsString(Map.of("dueAt",deadline))))
            .andExpect(status().isOk()).andExpect(jsonPath("$.dueAt").value(original.path("dueAt").asText()));
        TaskPolicyTestSupport.decide(mvc,json,id,"leader","approved");
        mvc.perform(get(path).header("Authorization",token("staff"))).andExpect(status().isOk())
            .andExpect(jsonPath("$.dueAt").value(deadline));
    }

    @Test void invalidResponsibleUsersDoNotPartiallyCreateATask() throws Exception {
        long before = taskCount();
        long staff = userId("staff");
        for (var invalid : List.of(input(staff,staff), input(staff,userId("team_lead")).put("assigneeId",userId("founder")))) {
            mvc.perform(post("/api/v1/tasks").header("Authorization",token("founder")).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(invalid))).andExpect(status().isBadRequest());
        }
        mvc.perform(post("/api/v1/tasks").header("Authorization",token("founder")).contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsString(input(staff,999999L)))).andExpect(status().isNotFound());
        assertThat(taskCount()).isEqualTo(before);
    }

    @Test void effectiveLabelsStayVisibleUntilTheSecondaryAssigneesManagerApproves() throws Exception {
        long oldLabel = label("Original");
        long newLabel = label("Replacement");
        var input = input(userId("staff"),userId("team_lead")).put("labelId",oldLabel);
        long id = create("founder",input).path("id").asLong();
        String path = "/api/v1/content-labels/task/" + id;
        mvc.perform(put(path).header("Authorization",token("team_lead")).contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsString(Map.of("labelIds",List.of(newLabel)))))
            .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("pending"))
            .andExpect(jsonPath("$.labels[0].id").value(oldLabel));
        mvc.perform(get("/api/v1/tasks/"+id).header("Authorization",token("staff"))).andExpect(status().isOk())
            .andExpect(jsonPath("$.label.id").value(oldLabel));
        long review = TaskPolicyTestSupport.pendingReview(mvc,json,id,"leader");
        mvc.perform(put("/api/v1/task-reviews/"+review).header("Authorization",token("founder")).contentType(MediaType.APPLICATION_JSON)
            .content("{\"status\":\"approved\"}")).andExpect(status().isForbidden());
        TaskPolicyTestSupport.decide(mvc,json,id,"leader","approved");
        mvc.perform(get("/api/v1/content-labels").header("Authorization",token("staff"))).andExpect(status().isOk())
            .andExpect(jsonPath("$[?(@.contentId == "+id+")].labels[0].id").value(org.hamcrest.Matchers.hasItem((int)newLabel)));
        mvc.perform(get("/api/v1/tasks/"+id).header("Authorization",token("staff"))).andExpect(status().isOk())
            .andExpect(jsonPath("$.label.id").value(newLabel));
        mvc.perform(put(path).header("Authorization",token("founder")).contentType(MediaType.APPLICATION_JSON)
            .content("{\"labelIds\":[]}")).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("applied"))
            .andExpect(jsonPath("$.labels").isEmpty());
        mvc.perform(get("/api/v1/tasks/"+id).header("Authorization",token("staff"))).andExpect(status().isOk())
            .andExpect(jsonPath("$.label").doesNotExist());
    }

    @Test void definitionEditDoesNotChangeEffectiveTagsBeforeApproval() throws Exception {
        long original = label("Before");
        long replacement = label("After");
        var body = input(userId("staff"),userId("team_lead")).put("labelId",original);
        var effective = TaskPolicyTestSupport.approveInitial(mvc,json,create("team_lead",body),"leader");
        long id = effective.path("id").asLong();
        String path = "/api/v1/tasks/" + id;
        body.putArray("labelIds").add(replacement);
        mvc.perform(put(path).header("Authorization",token("team_lead")).contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsString(body))).andExpect(status().isOk())
            .andExpect(jsonPath("$.reviewStatus").value("pending_change")).andExpect(jsonPath("$.label.id").value(original));
        mvc.perform(post(path+"/reject").header("Authorization",token("leader")).contentType(MediaType.APPLICATION_JSON)
            .content("{\"note\":\"Keep the original\"}")).andExpect(status().isOk()).andExpect(jsonPath("$.label.id").value(original));
        mvc.perform(put(path).header("Authorization",token("team_lead")).contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsString(body))).andExpect(status().isOk());
        mvc.perform(post(path+"/approve").header("Authorization",token("leader"))).andExpect(status().isOk())
            .andExpect(jsonPath("$.label.id").value(replacement));
    }

    private long userId(String name) { return db.queryForObject("select id from users where username=?",Long.class,name); }
    private long label(String name) {
        db.update("insert into work_labels(name,color,active) values(?,'#ABCDEF',true)",name);
        return db.queryForObject("select id from work_labels where name=?",Long.class,name);
    }
    private long taskCount() { return db.queryForObject("select count(*) from dispatch_tasks",Long.class); }
    private String token(String user) throws Exception { return TaskPolicyTestSupport.token(mvc,json,user); }
    private ObjectNode input(long primary,long secondary) {
        var now = Instant.now().truncatedTo(java.time.temporal.ChronoUnit.SECONDS);
        var body = json.createObjectNode().put("title","Shared task").put("assigneeId",primary)
            .put("startAt",now.toString()).put("dueAt",now.plusSeconds(86400).toString());
        body.putArray("assigneeIds").add(primary).add(secondary);
        return body;
    }
    private JsonNode create(String user,ObjectNode input) throws Exception {
        return json.readTree(mvc.perform(post("/api/v1/tasks").header("Authorization",token(user)).contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsString(input))).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());
    }
}
