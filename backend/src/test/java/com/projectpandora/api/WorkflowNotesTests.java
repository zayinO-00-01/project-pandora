package com.projectpandora.api;

import com.fasterxml.jackson.databind.*;
import com.projectpandora.api.task.*;
import com.projectpandora.api.user.*;
import com.projectpandora.api.notification.NotificationService;
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
class WorkflowNotesTests {
    @Autowired MockMvc mvc; @Autowired ObjectMapper json; @Autowired UserRepository users;
    @Autowired TaskRepository tasks; @Autowired NotificationService notifications;
    String token(String name) throws Exception {return TaskPolicyTestSupport.token(mvc,json,name);}
    long uid(String name) {return users.findByUsername(name).orElseThrow().getId();}
    JsonNode read(ResultActions result) throws Exception {return json.readTree(result.andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8));}
    long create(String creator,String assignee) throws Exception {
        return read(mvc.perform(post("/api/v1/tasks").header("Authorization",token(creator)).contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsString(Map.of("title","notes review task","assigneeId",uid(assignee))))).andExpect(status().isCreated())).get("id").asLong();
    }
    ResultActions detail(String who,long id) throws Exception {return mvc.perform(get("/api/v1/tasks/"+id).header("Authorization",token(who)));}
    ResultActions note(String who,long id) throws Exception {return mvc.perform(get("/api/v1/tasks/"+id+"/note").header("Authorization",token(who)));}
    ResultActions save(String who,long id,String content) throws Exception {return mvc.perform(put("/api/v1/tasks/"+id+"/note").header("Authorization",token(who)).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("content",content))));}
    long crossDepartmentReview() throws Exception {
        long id=create("staff","founder");
        mvc.perform(put("/api/v1/tasks/"+id+"/approval").header("Authorization",token("team_lead")).contentType(MediaType.APPLICATION_JSON).content("{\"decision\":\"approved\"}")).andExpect(status().isOk());
        mvc.perform(post("/api/v1/tasks/"+id+"/keywords").header("Authorization",token("founder")).contentType(MediaType.APPLICATION_JSON).content("{\"text\":\"original\"}")).andExpect(status().isOk());
        long keyword=tasks.findById(id).orElseThrow().getKeywords().get(0).getId();
        mvc.perform(put("/api/v1/tasks/"+id+"/keywords/"+keyword).header("Authorization",token("staff")).contentType(MediaType.APPLICATION_JSON).content("{\"text\":\"changed\"}")).andExpect(status().isOk());
        return id;
    }
    @Test void pendingKeywordReviewerReadsOnlySingleDetailAndLosesAccessAfterDecision() throws Exception {
        long id=crossDepartmentReview();
        detail("team_lead",id).andExpect(status().isOk()).andExpect(jsonPath("$.id").value(id));
        var list=read(mvc.perform(get("/api/v1/tasks").header("Authorization",token("team_lead"))).andExpect(status().isOk()));
        assertThat(list.findValuesAsText("id")).doesNotContain(""+id);
        detail("leader",id).andExpect(status().isForbidden());
        note("team_lead",id).andExpect(status().isForbidden());save("team_lead",id,"private").andExpect(status().isForbidden());
        TaskPolicyTestSupport.decide(mvc,json,id,"team_lead","approved");
        detail("team_lead",id).andExpect(status().isForbidden());
    }
    @Test void withdrawalRevokesTemporaryDetailAccess() throws Exception {
        long id=crossDepartmentReview();detail("team_lead",id).andExpect(status().isOk());
        TaskPolicyTestSupport.decide(mvc,json,id,"staff","canceled");detail("team_lead",id).andExpect(status().isForbidden());
    }
    @Test void pendingCrossDepartmentDefinitionReviewerMayReadOnlyDetailUntilWithdrawal() throws Exception {
        long id=create("staff","founder");
        mvc.perform(put("/api/v1/tasks/"+id+"/approval").header("Authorization",token("team_lead")).contentType(MediaType.APPLICATION_JSON).content("{\"decision\":\"approved\"}")).andExpect(status().isOk());
        mvc.perform(put("/api/v1/tasks/"+id).header("Authorization",token("staff")).contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsString(Map.of("title","proposed edit","assigneeId",uid("founder"))))).andExpect(status().isOk());
        detail("team_lead",id).andExpect(status().isOk());note("team_lead",id).andExpect(status().isForbidden());
        save("team_lead",id,"no temporary notes").andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/tasks/"+id+"/withdraw").header("Authorization",token("staff"))).andExpect(status().isOk());
        detail("team_lead",id).andExpect(status().isForbidden());
    }
    @Test void approvedUpwardViewPermissionAllowsExistingMemberTaskDetailAndPrivateNoteUntilRevocation() throws Exception {
        long id=create("founder","team_lead");detail("staff",id).andExpect(status().isForbidden());
        long requestId=read(mvc.perform(post("/api/v1/view-requests").header("Authorization",token("staff")).contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsString(Map.of("targetId",uid("team_lead"),"reason","review work")))).andExpect(status().isOk())).get("id").asLong();
        mvc.perform(put("/api/v1/view-requests/"+requestId).header("Authorization",token("team_lead")).contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"approved\"}")).andExpect(status().isOk());
        var memberTasks=read(mvc.perform(get("/api/v1/tasks").param("userId",""+uid("team_lead")).header("Authorization",token("staff"))).andExpect(status().isOk()));
        assertThat(memberTasks.findValuesAsText("id")).contains(""+id);
        detail("staff",id).andExpect(status().isOk());save("staff",id,"my upward task note").andExpect(status().isOk());
        note("staff",id).andExpect(status().isOk()).andExpect(jsonPath("$.content").value("my upward task note"));
        var ordinary=read(mvc.perform(get("/api/v1/tasks").header("Authorization",token("staff"))).andExpect(status().isOk()));
        assertThat(ordinary.findValuesAsText("id")).doesNotContain(""+id);
        mvc.perform(put("/api/v1/view-requests/"+requestId).header("Authorization",token("team_lead")).contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"revoked\"}")).andExpect(status().isOk());
        detail("staff",id).andExpect(status().isForbidden());note("staff",id).andExpect(status().isForbidden());
    }
    @Test void organizationChangesRecomputeTemporaryReviewerAndInvalidManagersHaveNoAccess() throws Exception {
        long id=crossDepartmentReview();var staff=users.findByUsername("staff").orElseThrow();staff.setManagerId(uid("leader"));users.saveAndFlush(staff);
        detail("team_lead",id).andExpect(status().isForbidden());detail("leader",id).andExpect(status().isOk());
        staff.setManagerId(null);users.saveAndFlush(staff);detail("leader",id).andExpect(status().isForbidden());
    }
    @Test void notesArePrivatePersistAcrossLoginAndBlankClears() throws Exception {
        long id=create("founder","staff");
        note("staff",id).andExpect(status().isOk()).andExpect(jsonPath("$.taskId").value(id)).andExpect(jsonPath("$.content").value("")).andExpect(jsonPath("$.updatedAt").isEmpty());
        save("staff",id,"my private note").andExpect(status().isOk()).andExpect(jsonPath("$.updatedAt").isNotEmpty());
        save("founder",id,"founder note").andExpect(status().isOk());
        note("staff",id).andExpect(status().isOk()).andExpect(jsonPath("$.content").value("my private note"));
        save("staff",id," \n\t ").andExpect(status().isOk()).andExpect(jsonPath("$.content").value(""));
        note("staff",id).andExpect(status().isOk()).andExpect(jsonPath("$.content").value(""));
        note("founder",id).andExpect(status().isOk()).andExpect(jsonPath("$.content").value("founder note"));
    }
    @Test void notesValidateLengthAndRequireApprovedVisibleBusinessTask() throws Exception {
        long id=create("founder","staff");save("staff",id,"x".repeat(2000)).andExpect(status().isOk());save("staff",id,"x".repeat(2001)).andExpect(status().isBadRequest());
        mvc.perform(put("/api/v1/tasks/"+id+"/note").header("Authorization",token("staff")).contentType(MediaType.APPLICATION_JSON).content("{\"content\":null}")).andExpect(status().isBadRequest());
        note("admin",id).andExpect(status().isForbidden());save("admin",id,"admin").andExpect(status().isForbidden());
        long pending=create("staff","founder");note("staff",pending).andExpect(status().isConflict());save("staff",pending,"no").andExpect(status().isConflict());
        mvc.perform(delete("/api/v1/tasks/"+id).header("Authorization",token("founder"))).andExpect(status().isNoContent());
        note("staff",id).andExpect(status().isNotFound());save("staff",id,"deleted").andExpect(status().isNotFound());detail("staff",id).andExpect(status().isNotFound());
    }
    @Test void notesBecomeUnavailableWhenNormalTaskVisibilityIsLost() throws Exception {
        long id=create("founder","staff");save("team_lead",id,"manager private").andExpect(status().isOk());
        var staff=users.findByUsername("staff").orElseThrow();staff.setManagerId(uid("leader"));users.saveAndFlush(staff);
        note("team_lead",id).andExpect(status().isForbidden());save("team_lead",id,"no").andExpect(status().isForbidden());
    }
    @Test void viewRequestCreationAndDecisionNotificationsPointToSameRequest() throws Exception {
        long requestId=read(mvc.perform(post("/api/v1/view-requests").header("Authorization",token("staff")).contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsString(Map.of("targetId",uid("team_lead"),"reason","review work")))).andExpect(status().isOk())).get("id").asLong();
        mvc.perform(put("/api/v1/view-requests/"+requestId).header("Authorization",token("team_lead")).contentType(MediaType.APPLICATION_JSON)
            .content("{\"status\":\"approved\"}")).andExpect(status().isOk());
        for(String who:new String[]{"team_lead","staff"}) {
            var items=read(mvc.perform(get("/api/v1/notifications").header("Authorization",token(who))).andExpect(status().isOk())).get("items");
            assertThat(items).hasSize(1);assertThat(items.get(0).get("taskId").isNull()).isTrue();
            assertThat(items.get(0).get("referenceId").asLong()).isEqualTo(requestId);
        }
    }
    @Test void viewRequestNotificationKeepsIndependentReferenceAndTaskNotificationsStayCompatible() throws Exception {
        notifications.send(uid("staff"),"view request","body","view-request",123L);
        notifications.send(uid("staff"),"task","body","task",456L);
        notifications.notifyUser(uid("staff"),"legacy","legacy","body",789L);
        var items=read(mvc.perform(get("/api/v1/notifications").header("Authorization",token("staff"))).andExpect(status().isOk())).get("items");
        for(var item:items) switch(item.get("type").asText()) {
            case "view-request" -> {assertThat(item.get("taskId").isNull()).isTrue();assertThat(item.get("referenceId").asLong()).isEqualTo(123);}
            case "task" -> {assertThat(item.get("taskId").asLong()).isEqualTo(456);assertThat(item.get("referenceId").isNull()).isTrue();}
            case "legacy" -> {assertThat(item.get("taskId").asLong()).isEqualTo(789);assertThat(item.get("referenceId").isNull()).isTrue();}
        }
    }
}
