package com.projectpandora.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Seed-account workflow helpers: tests opt in when they need an effective task. */
final class TaskPolicyTestSupport {
    private TaskPolicyTestSupport() {}
    static String token(MockMvc mvc,ObjectMapper json,String username) throws Exception {
        var response=mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsString(Map.of("username",username,"password","demo1234"))))
            .andExpect(status().isOk()).andReturn().getResponse();
        return "Bearer "+json.readTree(response.getContentAsString()).get("token").asText();
    }
    static JsonNode approveInitial(MockMvc mvc,ObjectMapper json,JsonNode task,String reviewer) throws Exception {
        if(!"pending".equals(task.path("approvalStatus").asText()))return task;
        return json.readTree(mvc.perform(put("/api/v1/tasks/"+task.get("id").asLong()+"/approval")
            .header("Authorization",token(mvc,json,reviewer)).contentType(MediaType.APPLICATION_JSON)
            .content("{\"decision\":\"approved\"}"))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8));
    }
    static long pendingReview(MockMvc mvc,ObjectMapper json,long taskId,String reviewer) throws Exception {
        var list=json.readTree(mvc.perform(get("/api/v1/task-reviews").header("Authorization",token(mvc,json,reviewer)))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8));
        for(var review:list)if(review.get("taskId").asLong()==taskId && "pending".equals(review.get("status").asText()))return review.get("id").asLong();
        throw new AssertionError("Missing pending review for task "+taskId);
    }
    static void decide(MockMvc mvc,ObjectMapper json,long taskId,String reviewer,String decision) throws Exception {
        long id=pendingReview(mvc,json,taskId,reviewer);
        mvc.perform(put("/api/v1/task-reviews/"+id).header("Authorization",token(mvc,json,reviewer))
            .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("status",decision))))
            .andExpect(status().isOk());
    }
    static JsonNode task(MockMvc mvc,ObjectMapper json,long id,String viewer) throws Exception {
        var list=json.readTree(mvc.perform(get("/api/v1/tasks").header("Authorization",token(mvc,json,viewer)))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8));
        for(var item:list)if(item.get("id").asLong()==id)return item;
        throw new AssertionError("Missing task "+id);
    }
}
