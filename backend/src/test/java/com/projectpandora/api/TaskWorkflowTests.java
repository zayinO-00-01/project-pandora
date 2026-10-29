package com.projectpandora.api;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.projectpandora.api.user.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test") @Transactional
class TaskWorkflowTests {
    @Autowired jakarta.persistence.EntityManager entityManager;
    @Autowired MockMvc mvc;@Autowired ObjectMapper json;@Autowired UserRepository users;
    String token(String name) throws Exception {
        var r=mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("username",name,"password","demo1234")))).andExpect(status().isOk()).andReturn();
        return "Bearer "+json.readTree(r.getResponse().getContentAsString()).get("token").asText();
    }
    JsonNode getList(String path,String token) throws Exception {var result=json.readTree(mvc.perform(get("/api/v1/"+path).header("Authorization",token)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8));return path.equals("notifications")?result.get("items"):result;}
    String body(String name,String assignee) throws Exception {return json.writeValueAsString(Map.of("title",name,"detail","工作说明","assigneeId",users.findByUsername(assignee).orElseThrow().getId(),"priority","high"));}
    JsonNode create(String actor,String title,String assignee) throws Exception {return json.readTree(mvc.perform(post("/api/v1/tasks").header("Authorization",token(actor)).contentType(MediaType.APPLICATION_JSON).content(body(title,assignee))).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8));}
    long latestReview(String actor) throws Exception {return getList("task-reviews",token(actor)).get(0).get("id").asLong();}
    void decision(String actor,long id,String status,int expected) throws Exception {mvc.perform(put("/api/v1/task-reviews/"+id).header("Authorization",token(actor)).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("status",status)))).andExpect(status().is(expected));}
    @Test void downwardDispatchAndSelfApprovalRemainDistinctFromKeywordReviews() throws Exception {
        mvc.perform(post("/api/v1/tasks").header("Authorization",token("staff")).contentType(MediaType.APPLICATION_JSON).content(body("越级工作","founder"))).andExpect(status().isForbidden());
        var task=create("staff","自主工作","staff");long id=task.get("id").asLong();
        assertThat(task.get("approvalStatus").asText()).isEqualTo("pending");
        assertThat(getList("task-reviews",token("staff")).isEmpty()).isTrue();
        mvc.perform(put("/api/v1/tasks/"+id+"/approval").header("Authorization",token("staff")).contentType(MediaType.APPLICATION_JSON).content("{\"decision\":\"approved\"}")).andExpect(status().isForbidden());
        mvc.perform(put("/api/v1/tasks/"+id+"/approval").header("Authorization",token("team_lead")).contentType(MediaType.APPLICATION_JSON).content("{\"decision\":\"approved\"}")).andExpect(status().isOk());
        assertThat(taskById("staff",id).get("approvalStatus").asText()).isEqualTo("approved");
    }
    @Test void editAndCompletionUseLifecycleReviewAndProgressAppliesImmediately() throws Exception {
        long id=create("team_lead","原事项","staff").get("id").asLong();
        mvc.perform(put("/api/v1/tasks/"+id).header("Authorization",token("team_lead")).contentType(MediaType.APPLICATION_JSON).content(body("编辑后的事项","staff"))).andExpect(status().isOk()).andExpect(jsonPath("$.title").value("原事项"));
        mvc.perform(post("/api/v1/tasks/"+id+"/reject").header("Authorization",token("leader")).contentType(MediaType.APPLICATION_JSON).content("{\"note\":\"保留原定义\"}")).andExpect(status().isOk());
        assertThat(taskById("staff",id).get("title").asText()).isEqualTo("原事项");
        mvc.perform(put("/api/v1/tasks/"+id+"/progress").header("Authorization",token("staff")).contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"doing\",\"progressNote\":\"执行中\"}")).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("doing")).andExpect(jsonPath("$.reviewStatus").value("none"));
        mvc.perform(put("/api/v1/tasks/"+id+"/progress").header("Authorization",token("staff")).contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"done\",\"progressNote\":\"全部完成\"}")).andExpect(status().isOk()).andExpect(jsonPath("$.reviewStatus").value("pending_done"));
        mvc.perform(post("/api/v1/tasks/"+id+"/approve").header("Authorization",token("team_lead"))).andExpect(status().isOk());
        assertThat(taskById("staff",id).get("reviewStatus").asText()).isEqualTo("approved");
        assertThat(getList("task-reviews",token("staff")).isEmpty()).isTrue();
    }
    @Test void keywordApplicantCanWithdrawAndResubmitButCannotApproveTheirOwnChange() throws Exception {
        long id=create("team_lead","关键词撤回","staff").get("id").asLong();keyword("team_lead",id,"原词",200);
        long key=taskById("staff",id).get("keywords").get(0).get("id").asLong();
        mvc.perform(put("/api/v1/tasks/"+id+"/keywords/"+key).header("Authorization",token("staff")).contentType(MediaType.APPLICATION_JSON).content("{\"text\":\"新词\"}")).andExpect(status().isOk());
        long review=latestReview("staff");decision("team_lead",review,"canceled",403);decision("staff",review,"canceled",200);
        assertThat(taskById("staff",id).get("keywords").get(0).get("text").asText()).isEqualTo("原词");
        mvc.perform(put("/api/v1/tasks/"+id+"/keywords/"+key).header("Authorization",token("staff")).contentType(MediaType.APPLICATION_JSON).content("{\"text\":\"重提词\"}")).andExpect(status().isOk());
        long resubmitted=latestReview("staff");decision("staff",resubmitted,"approved",403);decision("team_lead",resubmitted,"approved",200);decision("team_lead",resubmitted,"approved",409);
        assertThat(taskById("staff",id).get("keywords").get(0).get("text").asText()).isEqualTo("重提词");
    }
    @Test void founderEditsAndDeletesDirectlyWhileAssigneeCannotDelete() throws Exception {
        var task=create("founder","待删除事项","staff");long id=task.get("id").asLong();
        mvc.perform(delete("/api/v1/tasks/"+id).header("Authorization",token("staff"))).andExpect(status().isForbidden());
        mvc.perform(put("/api/v1/tasks/"+id).header("Authorization",token("founder")).contentType(MediaType.APPLICATION_JSON).content(body("创始人直接编辑","staff"))).andExpect(status().isOk()).andExpect(jsonPath("$.title").value("创始人直接编辑"));
        assertThat(getList("task-reviews",token("founder")).isEmpty()).isTrue();
        mvc.perform(delete("/api/v1/tasks/"+id).header("Authorization",token("founder"))).andExpect(status().isNoContent());
        assertThat(getList("tasks",token("staff")).findValuesAsText("id")).doesNotContain(""+id);
        assertThat(getList("notifications",token("staff")).findValuesAsText("title")).contains("任务已删除");
    }
    @Test void founderMaintainsLabelsEveryoneSelectsAndCalendarReceivesUpdatedColors() throws Exception {
        var label=json.readTree(mvc.perform(post("/api/v1/labels").header("Authorization",token("founder")).contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"潘多拉\",\"color\":\"#BCD3EF\"}")).andExpect(status().isOk()).andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8));
        long labelId=label.get("id").asLong();
        mvc.perform(post("/api/v1/labels").header("Authorization",token("staff")).contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"越权\",\"color\":\"#FFFFFF\"}")).andExpect(status().isForbidden());
        var data=json.readTree(body("带标签的任务","staff"));((com.fasterxml.jackson.databind.node.ObjectNode)data).put("labelId",labelId);
        mvc.perform(post("/api/v1/tasks").header("Authorization",token("founder")).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(data))).andExpect(status().isCreated()).andExpect(jsonPath("$.label.color").value("#BCD3EF"));
        mvc.perform(put("/api/v1/labels/"+labelId).header("Authorization",token("founder")).contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"品牌更新\",\"color\":\"#B9DAD0\"}")).andExpect(status().isOk());
        entityManager.flush();entityManager.clear();
        assertThat(getList("tasks",token("staff")).get(0).get("label").get("color").asText()).isEqualTo("#B9DAD0");
        String date=java.time.LocalDate.now(java.time.ZoneId.of("Asia/Shanghai")).toString();
        mvc.perform(post("/api/v1/logs").header("Authorization",token("staff")).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("logDate",date,"content","正常工作","labelId",labelId)))).andExpect(status().isCreated()).andExpect(jsonPath("$.label.name").value("品牌更新"));
        entityManager.flush();entityManager.clear();
        assertThat(getList("logs",token("staff")).get(0).get("label").get("name").asText()).isEqualTo("品牌更新");
        var notification=getList("notifications",token("staff")).get(0);
        mvc.perform(post("/api/v1/notifications/"+notification.get("id").asLong()+"/read").header("Authorization",token("founder"))).andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/notifications/"+notification.get("id").asLong()+"/read").header("Authorization",token("staff"))).andExpect(status().isNoContent());
    }

    JsonNode keyword(String actor,long taskId,String text,int expected) throws Exception {
        return json.readTree(mvc.perform(post("/api/v1/tasks/"+taskId+"/keywords").header("Authorization",token(actor)).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("text",text)))).andExpect(status().is(expected)).andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8));
    }
    JsonNode taskById(String actor,long id) throws Exception {
        entityManager.flush();entityManager.clear();
        for(var item:getList("tasks",token(actor)))if(item.get("id").asLong()==id)return item;
        throw new AssertionError("Task missing");
    }
    @Test void participantsCollaborateAndChangingAnotherAuthorsKeywordNeedsReview() throws Exception {
        long id=create("team_lead","协作关键词","staff").get("id").asLong();
        keyword("team_lead",id,"需求",200);keyword("staff",id,"联调",200);
        var list=taskById("staff",id).get("keywords");assertThat(list.size()).isEqualTo(2);
        long keywordId=list.get(0).get("id").asLong();
        mvc.perform(put("/api/v1/tasks/"+id+"/keywords/"+keywordId).header("Authorization",token("staff")).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("text","需求评审")))).andExpect(status().isOk());
        assertThat(taskById("staff",id).get("keywords").get(0).get("text").asText()).isEqualTo("需求");
        long review=latestReview("staff");decision("staff",review,"approved",403);decision("leader",review,"approved",403);decision("team_lead",review,"approved",200);
        assertThat(taskById("staff",id).get("keywords").get(0).get("text").asText()).isEqualTo("需求评审");
        keyword("leader",id,"旁观者",403);keyword("admin",id,"管理端",403);
        assertThat(getList("notifications",token("staff")).findValuesAsText("title")).contains("任务审核已批准");
    }
    @Test void ownerCanEditAndFounderCanDeleteWithoutReviewWhileForeignDeleteCanBeRejected() throws Exception {
        long id=create("founder","关键词修改和删除","staff").get("id").asLong();keyword("founder",id,"原始词",200);
        long key=taskById("staff",id).get("keywords").get(0).get("id").asLong();
        mvc.perform(delete("/api/v1/tasks/"+id+"/keywords/"+key).header("Authorization",token("staff"))).andExpect(status().isOk());
        long review=latestReview("staff");decision("team_lead",review,"rejected",200);
        assertThat(taskById("staff",id).get("keywords").size()).isEqualTo(1);
        mvc.perform(delete("/api/v1/tasks/"+id+"/keywords/"+key).header("Authorization",token("staff"))).andExpect(status().isOk());
        decision("team_lead",latestReview("staff"),"approved",200);
        assertThat(taskById("staff",id).get("keywords").isEmpty()).isTrue();
        keyword("staff",id,"自建词",200);key=taskById("staff",id).get("keywords").get(0).get("id").asLong();
        mvc.perform(put("/api/v1/tasks/"+id+"/keywords/"+key).header("Authorization",token("staff")).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("text","自己的新词")))).andExpect(status().isOk());
        assertThat(taskById("staff",id).get("keywords").get(0).get("text").asText()).isEqualTo("自己的新词");
        mvc.perform(delete("/api/v1/tasks/"+id+"/keywords/"+key).header("Authorization",token("founder"))).andExpect(status().isOk());
        assertThat(taskById("staff",id).get("keywords").isEmpty()).isTrue();
    }
    @Test void keywordValidationDuplicateLimitAndPendingMutationAreEnforced() throws Exception {
        long id=create("founder","校验","staff").get("id").asLong();
        keyword("staff",id,"   ",400);keyword("staff",id,"x".repeat(25),400);keyword("staff",id,"two\nlines",400);
        keyword("staff",id," API ",200);keyword("staff",id,"api",409);
        for(int i=0;i<11;i++)keyword("staff",id,"tag"+i,200);
        keyword("staff",id,"overflow",400);
        long foreign=create("staff","未获批","staff").get("id").asLong();keyword("staff",foreign,"draft",409);
        long key=taskById("staff",id).get("keywords").get(0).get("id").asLong();
        mvc.perform(delete("/api/v1/tasks/"+foreign+"/keywords/"+key).header("Authorization",token("founder"))).andExpect(status().isConflict());
        keyword("founder",id,"creator",400);
    }
    @Test void deletingTaskCancelsPendingKeywordReview() throws Exception {
        long id=create("founder","待取消的关键词审核","staff").get("id").asLong();keyword("founder",id,"原词",200);
        long key=taskById("staff",id).get("keywords").get(0).get("id").asLong();
        mvc.perform(delete("/api/v1/tasks/"+id+"/keywords/"+key).header("Authorization",token("staff"))).andExpect(status().isOk());
        long review=latestReview("staff");
        mvc.perform(delete("/api/v1/tasks/"+id).header("Authorization",token("founder"))).andExpect(status().isNoContent());
        assertThat(getList("task-reviews",token("staff")).get(0).get("status").asText()).isEqualTo("canceled");
        decision("team_lead",review,"approved",409);
    }
    @Test void managerChangeInvalidatesPendingKeywordApprovalAndApplicantCanCancel() throws Exception {
        long id=create("team_lead","组织变更关键词","staff").get("id").asLong();keyword("team_lead",id,"原词",200);
        long key=taskById("staff",id).get("keywords").get(0).get("id").asLong();
        mvc.perform(delete("/api/v1/tasks/"+id+"/keywords/"+key).header("Authorization",token("staff"))).andExpect(status().isOk());
        long review=latestReview("staff");var staff=users.findByUsername("staff").orElseThrow();staff.setManagerId(users.findByUsername("leader").orElseThrow().getId());users.saveAndFlush(staff);
        decision("team_lead",review,"approved",409);decision("leader",review,"approved",403);decision("staff",review,"canceled",200);
        assertThat(taskById("staff",id).get("keywords").size()).isEqualTo(1);
    }

    @Test void unrelatedProgressDoesNotStrandKeywordApprovalOrRejection() throws Exception {
        long id=create("team_lead","并行进度与关键词审核","staff").get("id").asLong();keyword("team_lead",id,"原词",200);
        long key=taskById("staff",id).get("keywords").get(0).get("id").asLong();
        for(String outcome:List.of("rejected","approved")) {
            mvc.perform(delete("/api/v1/tasks/"+id+"/keywords/"+key).header("Authorization",token("staff"))).andExpect(status().isOk());
            long review=latestReview("staff");
            mvc.perform(put("/api/v1/tasks/"+id+"/progress").header("Authorization",token("staff")).contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"doing\",\"progressNote\":\"继续执行\"}")).andExpect(status().isOk());
            decision("team_lead",review,outcome,200);
        }
        assertThat(taskById("staff",id).get("keywords").isEmpty()).isTrue();
    }

}
