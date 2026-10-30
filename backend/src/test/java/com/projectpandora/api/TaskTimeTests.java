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
    String token(String name) throws Exception {return TaskPolicyTestSupport.token(mvc,json,name);}
    com.fasterxml.jackson.databind.node.ObjectNode body() {
        return json.createObjectNode().put("title","时间安排").put("assigneeId",users.findByUsername("staff").orElseThrow().getId()).put("dueAt","2026-11-03T10:00:00Z");
    }
    JsonNode create(String actor,JsonNode body) throws Exception {
        return json.readTree(mvc.perform(post("/api/v1/tasks").header("Authorization",token(actor)).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)))
            .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8));
    }
    @Test void startIsOptionalDeadlineRequiredAndRangeValidated() throws Exception {
        assertThat(create("founder",body()).get("startAt").isNull()).isTrue();
        String start="2026-11-01T01:00:00Z";
        assertThat(create("founder",body().put("startAt",start)).get("startAt").asText()).isEqualTo(start);
        assertThat(create("founder",body().put("startAt","2026-11-03T10:00:00Z")).get("startAt").asText()).isEqualTo("2026-11-03T10:00:00Z");
        var missing=body();missing.remove("dueAt");
        for(var invalid:java.util.List.of(missing,body().putNull("dueAt"),body().put("startAt","2026-11-04T01:00:00Z"),body().put("startAt","invalid")))
            mvc.perform(post("/api/v1/tasks").header("Authorization",token("founder")).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(invalid))).andExpect(status().isBadRequest());
    }
    @Test void editingTimesWaitsForReviewThenPersistsAndCanClearStart() throws Exception {
        var original=create("staff",body().put("startAt","2026-11-01T01:00:00Z"));
        original=TaskPolicyTestSupport.approveInitial(mvc,json,original,"team_lead");
        long id=original.get("id").asLong();
        var change=body().put("startAt","2026-11-02T01:00:00Z");
        mvc.perform(put("/api/v1/tasks/"+id).header("Authorization",token("staff")).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(change)))
            .andExpect(status().isOk()).andExpect(jsonPath("$.startAt").value("2026-11-01T01:00:00Z"));
        mvc.perform(post("/api/v1/tasks/"+id+"/approve").header("Authorization",token("team_lead"))).andExpect(status().isOk());
        assertThat(TaskPolicyTestSupport.task(mvc,json,id,"staff").get("startAt").asText()).isEqualTo("2026-11-02T01:00:00Z");
        mvc.perform(put("/api/v1/tasks/"+id).header("Authorization",token("founder")).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body().putNull("startAt"))))
            .andExpect(status().isOk()).andExpect(jsonPath("$.startAt").isEmpty());
        var invalid=body();invalid.remove("dueAt");
        mvc.perform(put("/api/v1/tasks/"+id).header("Authorization",token("founder")).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(invalid))).andExpect(status().isBadRequest());
    }
}
