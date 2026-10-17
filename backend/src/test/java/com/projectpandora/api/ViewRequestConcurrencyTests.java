package com.projectpandora.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties="spring.datasource.url=jdbc:h2:mem:pandora_concurrency;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ViewRequestConcurrencyTests {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    private static final AtomicInteger sequence=new AtomicInteger();
    JsonNode login(String name) throws Exception {
        return json.readTree(mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsString(Map.of("username",name,"password","demo1234")))).andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString());
    }
    String token(JsonNode user) {return "Bearer "+user.get("token").asText();}
    JsonNode newStaff(JsonNode admin,JsonNode manager) throws Exception {
        String name="concurrent_"+sequence.incrementAndGet();
        mvc.perform(post("/api/v1/admin/users").header("Authorization",token(admin)).contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsString(Map.of("username",name,"password","demo1234","role","STAFF","managerId",manager.get("userId").asLong()))))
            .andExpect(status().isCreated());return login(name);
    }
    String requestBody(JsonNode manager) throws Exception {
        return json.writeValueAsString(Map.of("targetId",manager.get("userId").asLong(),"reason","并发审批验证"));
    }
    long request(JsonNode staff,JsonNode manager) throws Exception {
        return json.readTree(mvc.perform(post("/api/v1/view-requests").header("Authorization",token(staff)).contentType(MediaType.APPLICATION_JSON)
            .content(requestBody(manager))).andExpect(status().isOk()).andReturn().getResponse().getContentAsString()).get("id").asLong();
    }
    List<Integer> race(Callable<Integer> first,Callable<Integer> second) throws Exception {
        var start=new CountDownLatch(1);var ready=new CountDownLatch(2);var executor=Executors.newFixedThreadPool(2);
        try {
            var a=executor.submit(()->{ready.countDown();start.await();return first.call();});
            var b=executor.submit(()->{ready.countDown();start.await();return second.call();});
            assertThat(ready.await(5,TimeUnit.SECONDS)).isTrue();start.countDown();
            return List.of(a.get(15,TimeUnit.SECONDS),b.get(15,TimeUnit.SECONDS));
        } finally {executor.shutdownNow();}
    }
    @Test void duplicateConcurrentRequestsCreateOnePendingRecord() throws Exception {
        var admin=login("admin");var manager=login("team_lead");var staff=newStaff(admin,manager);
        String body=requestBody(manager);
        Callable<Integer> create=()->mvc.perform(post("/api/v1/view-requests").header("Authorization",token(staff))
            .contentType(MediaType.APPLICATION_JSON).content(body)).andReturn().getResponse().getStatus();
        assertThat(race(create,create)).containsExactlyInAnyOrder(200,409);
        mvc.perform(get("/api/v1/view-requests").header("Authorization",token(staff)))
            .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1)).andExpect(jsonPath("$[0].status").value("pending"));
    }
    @Test void competingDecisionsApplyOnlyOneTransition() throws Exception {
        var admin=login("admin");var manager=login("team_lead");var staff=newStaff(admin,manager);long id=request(staff,manager);
        Callable<Integer> approve=()->mvc.perform(put("/api/v1/view-requests/"+id).header("Authorization",token(manager))
            .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"approved\"}")).andReturn().getResponse().getStatus();
        Callable<Integer> deny=()->mvc.perform(put("/api/v1/view-requests/"+id).header("Authorization",token(manager))
            .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"denied\"}")).andReturn().getResponse().getStatus();
        assertThat(race(approve,deny)).containsExactlyInAnyOrder(200,409);
    }
    @Test void approvalRacingAccountDisableCannotLeaveAnActiveGrant() throws Exception {
        var admin=login("admin");var manager=login("team_lead");var staff=newStaff(admin,manager);long id=request(staff,manager);
        Callable<Integer> approve=()->mvc.perform(put("/api/v1/view-requests/"+id).header("Authorization",token(manager))
            .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"approved\"}")).andReturn().getResponse().getStatus();
        Callable<Integer> disable=()->mvc.perform(patch("/api/v1/admin/users/"+staff.get("userId").asLong()).header("Authorization",token(admin))
            .contentType(MediaType.APPLICATION_JSON).content("{\"disabled\":true}")).andReturn().getResponse().getStatus();
        var outcomes=race(approve,disable);assertThat(outcomes.get(0)).isIn(200,409);assertThat(outcomes.get(1)).isEqualTo(200);
        mvc.perform(patch("/api/v1/admin/users/"+staff.get("userId").asLong()).header("Authorization",token(admin))
            .contentType(MediaType.APPLICATION_JSON).content("{\"disabled\":false}")).andExpect(status().isOk());
        mvc.perform(get("/api/v1/logs").param("userId",manager.get("userId").asText()).header("Authorization",token(staff))).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/view-requests").header("Authorization",token(staff))).andExpect(status().isOk())
            .andExpect(jsonPath("$[0].status").value("revoked"));
    }
    @Test void concurrentTrimmedUsernamesReturnOneCreationAndOneClientError() throws Exception {
        var admin=login("admin");String name="duplicate_"+sequence.incrementAndGet();
        Callable<Integer> first=()->mvc.perform(post("/api/v1/admin/users").header("Authorization",token(admin)).contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsString(Map.of("username",name,"password","demo1234","role","STAFF")))).andReturn().getResponse().getStatus();
        Callable<Integer> second=()->mvc.perform(post("/api/v1/admin/users").header("Authorization",token(admin)).contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsString(Map.of("username"," "+name+" ","password","demo1234","role","STAFF")))).andReturn().getResponse().getStatus();
        assertThat(race(first,second)).containsExactlyInAnyOrder(201,400);
    }
}
