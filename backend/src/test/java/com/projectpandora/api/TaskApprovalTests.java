package com.projectpandora.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.projectpandora.api.security.JwtService;
import com.projectpandora.api.user.*;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test") @Transactional
class TaskApprovalTests {
    @Autowired MockMvc mvc; @Autowired ObjectMapper json; @Autowired UserRepository users; @Autowired JwtService jwt;
    UserEntity user(String name,Role role,UserEntity manager) {
        var u=new UserEntity();u.setUsername("approval-task-"+name);u.setPasswordHash("unused");u.setRole(role);
        u.setDisplayName(name);u.setManagerId(manager==null?null:manager.getId());return users.saveAndFlush(u);
    }
    String token(UserEntity u) {return "Bearer "+jwt.createToken(u.getId(),u.getUsername(),u.getRole());}
    JsonNode read(ResultActions result) throws Exception {return json.readTree(result.andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8));}
    ResultActions create(UserEntity creator,UserEntity assignee) throws Exception {
        return mvc.perform(post("/api/v1/tasks").header("Authorization",token(creator)).contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsString(Map.of("title","计划任务","assigneeId",assignee.getId()))));
    }
    ResultActions decide(UserEntity reviewer,long id,String decision,String note) throws Exception {
        return mvc.perform(put("/api/v1/tasks/"+id+"/approval").header("Authorization",token(reviewer)).contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsString(Map.of("decision",decision,"note",note))));
    }
    ResultActions feedback(UserEntity author,long id) throws Exception {
        return mvc.perform(put("/api/v1/tasks/"+id+"/progress").header("Authorization",token(author)).contentType(MediaType.APPLICATION_JSON)
            .content("{\"status\":\"doing\",\"progressNote\":\"开始执行\"}"));
    }
    ResultActions pending(UserEntity reviewer) throws Exception {return mvc.perform(get("/api/v1/tasks/pending-approvals").header("Authorization",token(reviewer)));}
    @Test void staffSelfTaskRequiresDirectManagerApprovalBeforeProgress() throws Exception {
        var manager=user("manager",Role.TEAM_LEAD,null);var staff=user("staff",Role.STAFF,manager);
        var task=read(create(staff,staff).andExpect(status().isCreated()).andExpect(jsonPath("$.approvalStatus").value("pending")));
        long id=task.get("id").asLong();assertThat(task.get("reviewerId").isNull()).isTrue();assertThat(task.get("reviewedAt").isNull()).isTrue();
        feedback(staff,id).andExpect(status().isConflict());
        pending(manager).andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(id));
        var approved=read(decide(manager,id,"approved","可以执行").andExpect(status().isOk()).andExpect(jsonPath("$.approvalStatus").value("approved"))
            .andExpect(jsonPath("$.reviewerId").value(manager.getId())));
        assertThat(approved.get("reviewedAt").asText()).isNotBlank();assertThat(approved.get("approvalNote").asText()).isEqualTo("可以执行");
        for(String field:new String[]{"status","progress","progressNote","createdBy","assigneeId","createdAt"})assertThat(approved.get(field)).as(field).isEqualTo(task.get(field));
        feedback(staff,id).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("todo"));
        var progressReview=read(mvc.perform(get("/api/v1/task-reviews").header("Authorization",token(manager))).andExpect(status().isOk())).get(0).get("id").asLong();
        mvc.perform(put("/api/v1/task-reviews/"+progressReview).header("Authorization",token(manager)).contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"approved\"}")).andExpect(status().isOk());
        decide(manager,id,"rejected","").andExpect(status().isConflict());pending(manager).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
    }
    @Test void rejectedTaskRemainsVisibleButCannotExecute() throws Exception {
        var manager=user("manager",Role.TEAM_LEAD,null);var staff=user("staff",Role.STAFF,manager);
        long id=read(create(staff,staff).andExpect(status().isCreated())).get("id").asLong();
        decide(manager,id,"rejected","").andExpect(status().isOk()).andExpect(jsonPath("$.approvalStatus").value("rejected"));feedback(staff,id).andExpect(status().isConflict());
        mvc.perform(get("/api/v1/tasks").header("Authorization",token(staff))).andExpect(status().isOk()).andExpect(jsonPath("$[0].approvalStatus").value("rejected"));
        decide(manager,id,"approved","").andExpect(status().isConflict());
    }
    @Test void onlyCurrentDirectManagerMayReview() throws Exception {
        var founder=user("founder",Role.FOUNDER,null);var department=user("department",Role.DEPT_HEAD,founder);
        var manager=user("manager",Role.TEAM_LEAD,department);var next=user("next",Role.TEAM_LEAD,department);var staff=user("staff",Role.STAFF,manager);var admin=user("admin",Role.ADMIN,null);
        long id=read(create(staff,staff).andExpect(status().isCreated())).get("id").asLong();
        for(var other:new UserEntity[]{staff,next,department,founder,admin})decide(other,id,"approved","").andExpect(status().isForbidden());
        pending(admin).andExpect(status().isForbidden());pending(founder).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
        staff.setManagerId(next.getId());users.saveAndFlush(staff);
        pending(manager).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));pending(next).andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(id));
        decide(manager,id,"approved","").andExpect(status().isForbidden());decide(next,id,"approved","").andExpect(status().isOk());
    }
    @Test void allBusinessRolesCanSelfPlanAndFounderNeedsNoReview() throws Exception {
        var founder=user("founder",Role.FOUNDER,null);var department=user("department",Role.DEPT_HEAD,founder);var team=user("team",Role.TEAM_LEAD,department);var staff=user("staff",Role.STAFF,team);
        for(var author:new UserEntity[]{department,team,staff})create(author,author).andExpect(status().isCreated()).andExpect(jsonPath("$.approvalStatus").value("pending"));
        var task=read(create(founder,founder).andExpect(status().isCreated()).andExpect(jsonPath("$.approvalStatus").value("approved")));
        feedback(founder,task.get("id").asLong()).andExpect(status().isOk());create(team,staff).andExpect(status().isCreated()).andExpect(jsonPath("$.approvalStatus").value("pending"));
        var admin=user("admin",Role.ADMIN,null);create(admin,admin).andExpect(status().isForbidden());create(founder,admin).andExpect(status().isForbidden());
        var disabled=user("disabled",Role.STAFF,team);disabled.setDisabled(true);users.saveAndFlush(disabled);create(founder,disabled).andExpect(status().isForbidden());
        create(staff,team).andExpect(status().isCreated()).andExpect(jsonPath("$.approvalStatus").value("pending"));
    }
    @Test void missingDisabledOrInvalidManagerPreventsNonFounderSelfTasks() throws Exception {
        var author=user("staff",Role.STAFF,null);create(author,author).andExpect(status().isBadRequest());
        var admin=user("admin",Role.ADMIN,null);author.setManagerId(admin.getId());users.saveAndFlush(author);create(author,author).andExpect(status().isBadRequest());
        var peer=user("peer",Role.STAFF,null);author.setManagerId(peer.getId());users.saveAndFlush(author);create(author,author).andExpect(status().isBadRequest());
        var manager=user("manager",Role.TEAM_LEAD,null);manager.setDisabled(true);users.saveAndFlush(manager);author.setManagerId(manager.getId());users.saveAndFlush(author);
        create(author,author).andExpect(status().isBadRequest());author.setManagerId(Long.MAX_VALUE);users.saveAndFlush(author);create(author,author).andExpect(status().isBadRequest());
    }
    @Test void reviewValidationDoesNotChangePendingTask() throws Exception {
        var manager=user("manager",Role.TEAM_LEAD,null);var staff=user("staff",Role.STAFF,manager);long id=read(create(staff,staff).andExpect(status().isCreated())).get("id").asLong();
        decide(manager,id,"pending","").andExpect(status().isBadRequest());decide(manager,id,"rejected","x".repeat(501)).andExpect(status().isBadRequest());
        pending(manager).andExpect(status().isOk()).andExpect(jsonPath("$[0].approvalStatus").value("pending"));decide(manager,id,"rejected","x".repeat(500)).andExpect(status().isOk());
    }
    @Test void panelLimitIsAppliedAfterApprovalFilter() throws Exception {
        var founder=user("founder",Role.FOUNDER,null);var manager=user("manager",Role.TEAM_LEAD,founder);var staff=user("staff",Role.STAFF,manager);
        for(int i=0;i<10;i++)create(founder,staff).andExpect(status().isCreated());for(int i=0;i<12;i++)create(staff,staff).andExpect(status().isCreated());
        mvc.perform(get("/api/v1/panels/map").header("Authorization",token(staff))).andExpect(status().isOk()).andExpect(jsonPath("$.companyDispatch.length()").value(10));
        mvc.perform(get("/api/v1/tasks").header("Authorization",token(staff))).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(22));
    }
    @Test void selfTaskCompletionNeedsCurrentManagerAndKeepsInitialApprovalAudit() throws Exception {
        var founder=user("founder",Role.FOUNDER,null);var department=user("department",Role.DEPT_HEAD,founder);
        var manager=user("manager",Role.TEAM_LEAD,department);var next=user("next",Role.TEAM_LEAD,department);
        var staff=user("staff",Role.STAFF,manager);
        long id=read(create(staff,staff).andExpect(status().isCreated())).get("id").asLong();
        var approved=read(decide(manager,id,"approved","initial approval").andExpect(status().isOk()));
        staff.setManagerId(next.getId());users.saveAndFlush(staff);
        mvc.perform(put("/api/v1/tasks/"+id+"/progress").header("Authorization",token(staff)).contentType(MediaType.APPLICATION_JSON)
            .content("{\"status\":\"done\",\"progressNote\":\"finished\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.reviewStatus").value("none")).andExpect(jsonPath("$.status").value("todo"));
        var review=read(mvc.perform(get("/api/v1/task-reviews").header("Authorization",token(next))).andExpect(status().isOk())).get(0).get("id").asLong();
        for(var forbidden:new UserEntity[]{staff,manager,founder})
            mvc.perform(put("/api/v1/task-reviews/"+review).header("Authorization",token(forbidden)).contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"approved\"}")).andExpect(status().isForbidden());
        mvc.perform(put("/api/v1/task-reviews/"+review).header("Authorization",token(next)).contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"approved\"}")).andExpect(status().isOk());
        var completed=read(mvc.perform(get("/api/v1/tasks").header("Authorization",token(staff))).andExpect(status().isOk())).get(0);
        assertThat(completed.get("status").asText()).isEqualTo("done");
        assertThat(completed.get("reviewStatus").asText()).isEqualTo("none");
        assertThat(completed.get("reviewedBy").asLong()).isEqualTo(next.getId());
        assertThat(completed.get("reviewedAt")).isEqualTo(approved.get("reviewedAt"));
        assertThat(completed.get("reviewerId")).isEqualTo(approved.get("reviewerId"));
        assertThat(completed.get("approvalNote")).isEqualTo(approved.get("approvalNote"));
        assertThat(completed.get("lifecycleReviewedAt").asText()).isNotBlank();
    }
    @Test void deletedPendingSelfTaskLeavesApprovalInboxAndCannotBeApproved() throws Exception {
        var manager=user("manager",Role.TEAM_LEAD,null);var staff=user("staff",Role.STAFF,manager);
        long id=read(create(staff,staff).andExpect(status().isCreated())).get("id").asLong();
        mvc.perform(delete("/api/v1/tasks/"+id).header("Authorization",token(staff))).andExpect(status().isNoContent());
        pending(manager).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
        decide(manager,id,"approved","").andExpect(status().isNotFound());
    }

}
