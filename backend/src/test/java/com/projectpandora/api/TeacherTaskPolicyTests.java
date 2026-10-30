package com.projectpandora.api;

import com.fasterxml.jackson.databind.*;
import com.projectpandora.api.user.*;
import com.projectpandora.api.task.TaskRepository;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.*;
import org.springframework.transaction.annotation.Transactional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test") @Transactional
class TeacherTaskPolicyTests {
    @Autowired MockMvc mvc; @Autowired ObjectMapper json; @Autowired UserRepository users; @Autowired TaskRepository tasks;
    String token(String name) throws Exception {return "Bearer "+read(mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("username",name,"password","demo1234")))).andExpect(status().isOk())).get("token").asText();}
    long uid(String name) {return users.findByUsername(name).orElseThrow().getId();}
    JsonNode read(ResultActions r) throws Exception {return json.readTree(r.andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8));}
    String body(String title,String assignee) throws Exception {return json.writeValueAsString(Map.of("dueAt","2026-11-01T10:00:00Z","title",title,"assigneeId",uid(assignee)));}
    JsonNode create(String creator,String assignee) throws Exception {return read(mvc.perform(post("/api/v1/tasks").header("Authorization",token(creator)).contentType(MediaType.APPLICATION_JSON).content(body("policy task",assignee))).andExpect(status().isCreated()));}
    JsonNode list(String actor,String path) throws Exception {return read(mvc.perform(get("/api/v1/"+path).header("Authorization",token(actor))).andExpect(status().isOk()));}
    JsonNode initial(String actor,long id) throws Exception {return read(mvc.perform(put("/api/v1/tasks/"+id+"/approval").header("Authorization",token(actor)).contentType(MediaType.APPLICATION_JSON).content("{\"decision\":\"approved\",\"note\":\"initial audit\"}")).andExpect(status().isOk()));}
    ResultActions progress(String actor,long id,String state) throws Exception {return mvc.perform(put("/api/v1/tasks/"+id+"/progress").header("Authorization",token(actor)).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("status",state,"progressNote","proposed feedback"))));}
    ResultActions decide(String actor,long id,String state) throws Exception {return mvc.perform(put("/api/v1/task-reviews/"+id).header("Authorization",token(actor)).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("status",state))));}
    JsonNode ownTask(String actor,long id) throws Exception {for(var task:list(actor,"tasks"))if(task.get("id").asLong()==id)return task;throw new AssertionError("task missing");}
    @Test void anyBusinessCreatorMaySelectCompanyMembersButOnlyApplicantAndCurrentReviewerSeePending() throws Exception {
        var task=create("staff","founder");long id=task.get("id").asLong();
        assertThat(task.get("approvalStatus").asText()).isEqualTo("pending");
        assertThat(list("staff","users/organization").get("assignees").findValuesAsText("username")).contains("founder","leader","team_lead","staff").doesNotContain("admin");
        assertThat(list("staff","tasks").findValuesAsText("id")).contains(""+id);
        assertThat(list("team_lead","tasks").findValuesAsText("id")).contains(""+id);
        assertThat(list("founder","tasks").findValuesAsText("id")).doesNotContain(""+id);
        assertThat(list("leader","tasks").findValuesAsText("id")).doesNotContain(""+id);
        progress("founder",id,"doing").andExpect(status().isConflict());
        initial("team_lead",id);
        assertThat(list("founder","tasks").findValuesAsText("id")).contains(""+id);
    }
    @Test void everyNonFounderCreationIncludesDownwardAssignmentsInInitialApproval() throws Exception {
        var task=create("team_lead","staff");long id=task.get("id").asLong();
        assertThat(task.get("approvalStatus").asText()).isEqualTo("pending");
        assertThat(list("staff","tasks").findValuesAsText("id")).doesNotContain(""+id);
        assertThat(list("leader","tasks/pending-approvals").findValuesAsText("id")).contains(""+id);
        initial("leader",id);assertThat(ownTask("staff",id).get("approvalStatus").asText()).isEqualTo("approved");
    }
    @Test void founderCreatedTaskDoesNotExemptStaffProgressAndManagerApprovalAloneAppliesIt() throws Exception {
        var task=create("founder","staff");long id=task.get("id").asLong();
        var unchanged=read(progress("staff",id,"done").andExpect(status().isOk()));
        assertThat(unchanged.get("status").asText()).isEqualTo("todo");assertThat(unchanged.get("progressNote").asText()).isEmpty();
        var review=list("staff","task-reviews").get(0);assertThat(review.get("operation").asText()).isEqualTo("PROGRESS");
        assertThat(json.readTree(review.get("payload").asText()).get("status").asText()).isEqualTo("done");
        decide("staff",review.get("id").asLong(),"approved").andExpect(status().isForbidden());
        decide("founder",review.get("id").asLong(),"approved").andExpect(status().isForbidden());
        progress("staff",id,"doing").andExpect(status().isConflict());
        decide("team_lead",review.get("id").asLong(),"approved").andExpect(status().isOk());
        var done=ownTask("staff",id);assertThat(done.get("status").asText()).isEqualTo("done");assertThat(done.get("reviewStatus").asText()).isEqualTo("none");
        assertThat(done.get("reviewedAt")).isEqualTo(task.get("reviewedAt"));
        assertThat(list("team_lead","tasks/reviews").isEmpty()).isTrue();
    }
    @Test void rejectedAndWithdrawnProgressPreserveEffectiveFeedbackAndMayBeResubmitted() throws Exception {
        long id=create("founder","staff").get("id").asLong();
        for(String result:new String[]{"rejected","canceled"}) {
            progress("staff",id,"doing").andExpect(status().isOk());long review=list("staff","task-reviews").get(0).get("id").asLong();
            decide(result.equals("canceled")?"staff":"team_lead",review,result).andExpect(status().isOk());
            assertThat(ownTask("staff",id).get("status").asText()).isEqualTo("todo");
        }
        assertThat(list("staff","task-reviews").size()).isEqualTo(2);
    }
    @Test void currentManagerReceivesProgressAfterOrganizationChangeAndOldManagerLosesAuthority() throws Exception {
        long id=create("founder","staff").get("id").asLong();progress("staff",id,"doing").andExpect(status().isOk());
        long review=list("staff","task-reviews").get(0).get("id").asLong();var staff=users.findByUsername("staff").orElseThrow();staff.setManagerId(uid("leader"));users.saveAndFlush(staff);
        assertThat(list("leader","task-reviews").get(0).get("reviewerId").asLong()).isEqualTo(uid("leader"));
        assertThat(list("team_lead","task-reviews").findValuesAsText("id")).doesNotContain(""+review);
        decide("team_lead",review,"approved").andExpect(status().isForbidden());decide("leader",review,"approved").andExpect(status().isOk());
        assertThat(ownTask("staff",id).get("status").asText()).isEqualTo("doing");
    }
    @Test void founderActorMayUpdateOwnProgressAndEditAnotherCreatorsDefinitionDirectly() throws Exception {
        long own=create("founder","founder").get("id").asLong();progress("founder",own,"done").andExpect(status().isOk()).andExpect(jsonPath("$.status").value("done"));
        progress("founder",own,"doing").andExpect(status().isOk()).andExpect(jsonPath("$.status").value("doing"));
        var other=create("team_lead","staff");long id=other.get("id").asLong();initial("leader",id);
        mvc.perform(put("/api/v1/tasks/"+id).header("Authorization",token("founder")).contentType(MediaType.APPLICATION_JSON).content(body("founder edit","staff"))).andExpect(status().isOk()).andExpect(jsonPath("$.title").value("founder edit"));
        assertThat(list("founder","task-reviews").isEmpty()).isTrue();
    }
    @Test void applicantCanWithdrawInitialAndDefinitionRequestThenEditAndResubmit() throws Exception {
        long id=create("staff","founder").get("id").asLong();
        mvc.perform(post("/api/v1/tasks/"+id+"/withdraw").header("Authorization",token("staff"))).andExpect(status().isOk()).andExpect(jsonPath("$.approvalStatus").value("rejected"));
        mvc.perform(put("/api/v1/tasks/"+id).header("Authorization",token("staff")).contentType(MediaType.APPLICATION_JSON).content(body("resubmitted","founder"))).andExpect(status().isOk()).andExpect(jsonPath("$.approvalStatus").value("pending"));
        initial("team_lead",id);
        mvc.perform(put("/api/v1/tasks/"+id).header("Authorization",token("staff")).contentType(MediaType.APPLICATION_JSON).content(body("pending definition","founder"))).andExpect(status().isOk()).andExpect(jsonPath("$.title").value("resubmitted"));
        mvc.perform(post("/api/v1/tasks/"+id+"/withdraw").header("Authorization",token("staff"))).andExpect(status().isOk()).andExpect(jsonPath("$.reviewStatus").value("none")).andExpect(jsonPath("$.title").value("resubmitted"));
    }
    @Test void genericAndLifecyclePendingRequestsPreventConflictingMutations() throws Exception {
        long id=create("founder","staff").get("id").asLong();progress("staff",id,"doing").andExpect(status().isOk());
        mvc.perform(put("/api/v1/tasks/"+id).header("Authorization",token("founder")).contentType(MediaType.APPLICATION_JSON).content(body("conflicting","staff"))).andExpect(status().isConflict());
        mvc.perform(post("/api/v1/tasks/"+id+"/keywords").header("Authorization",token("staff")).contentType(MediaType.APPLICATION_JSON).content("{\"text\":\"conflict\"}")).andExpect(status().isConflict());
    }
    @Test void deletingUnapprovedTaskDoesNotExposeItThroughAssigneeNotifications() throws Exception {
        var before=list("staff","notifications");
        for(String actor:new String[]{"team_lead","founder"}) {
            long id=create("team_lead","staff").get("id").asLong();
            assertThat(list("staff","tasks").findValuesAsText("id")).doesNotContain(""+id);
            mvc.perform(delete("/api/v1/tasks/"+id).header("Authorization",token(actor))).andExpect(status().isNoContent());
            assertThat(list("staff","notifications")).isEqualTo(before);
            assertThat(list("leader","tasks/pending-approvals").findValuesAsText("id")).doesNotContain(""+id);
        }
    }
    @Test void invalidCurrentManagerIsNotAdvertisedAsOldReviewerAndApplicantCanWithdraw() throws Exception {
        long id=create("founder","staff").get("id").asLong();progress("staff",id,"doing").andExpect(status().isOk());
        long review=list("staff","task-reviews").get(0).get("id").asLong();
        var staff=users.findByUsername("staff").orElseThrow();staff.setManagerId(null);users.saveAndFlush(staff);
        assertThat(list("staff","task-reviews").get(0).get("reviewerId").isNull()).isTrue();
        assertThat(list("team_lead","task-reviews").isEmpty()).isTrue();
        decide("team_lead",review,"approved").andExpect(status().isForbidden());
        decide("staff",review,"canceled").andExpect(status().isOk());
        assertThat(ownTask("staff",id).get("status").asText()).isEqualTo("todo");
    }
}
