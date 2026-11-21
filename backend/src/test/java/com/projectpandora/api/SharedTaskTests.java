package com.projectpandora.api;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.projectpandora.api.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import java.time.Instant;
import java.util.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test") @Transactional
class SharedTaskTests {
 @Autowired MockMvc mvc; @Autowired ObjectMapper json; @Autowired UserRepository users;
 @Autowired org.springframework.jdbc.core.JdbcTemplate db;
 String token(String name)throws Exception{return TaskPolicyTestSupport.token(mvc,json,name);}
 @Test void oneTaskIsVisibleToBothPeopleAndSharesApprovedProgress()throws Exception {
  long staff=users.findByUsername("staff").orElseThrow().getId(),lead=users.findByUsername("team_lead").orElseThrow().getId();
  var due=Instant.now().plusSeconds(86400).toString();
  var input=Map.of("title","共同执行","assigneeId",staff,"assigneeIds",List.of(staff,lead),"dueAt",due,"startAt",due);
  long before=db.queryForObject("select count(*) from dispatch_tasks",Long.class);
  var response=mvc.perform(post("/api/v1/tasks").header("Authorization",token("founder")).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(input)))
   .andExpect(status().isCreated()).andExpect(jsonPath("$.assigneeIds.length()").value(2)).andReturn();
  long id=json.readTree(response.getResponse().getContentAsString()).get("id").asLong();
  assertThat(db.queryForObject("select count(*) from dispatch_tasks",Long.class)).isEqualTo(before+1);
  for(String person:List.of("staff","team_lead")) {
   var list=mvc.perform(get("/api/v1/tasks").header("Authorization",token(person))).andExpect(status().isOk()).andReturn();
   assertThat(json.readTree(list.getResponse().getContentAsString()).findValues("id").stream().filter(n->n.asLong()==id).count()).isEqualTo(1);
   mvc.perform(get("/api/v1/tasks/"+id).header("Authorization",token(person))).andExpect(status().isOk());
  }
  assertThat(db.queryForObject("select count(*) from notifications where task_id=? and type='task_assigned'",Long.class,id)).isEqualTo(2);
  // The secondary responsible person can report; their own manager reviews the shared progress.
  mvc.perform(put("/api/v1/tasks/"+id+"/progress").header("Authorization",token("team_lead")).contentType(MediaType.APPLICATION_JSON)
   .content("{\"status\":\"doing\",\"progressNote\":\"共同推进\"}")).andExpect(status().isOk());
  var reviews=mvc.perform(get("/api/v1/task-reviews").header("Authorization",token("leader"))).andExpect(status().isOk()).andReturn();
  long review=json.readTree(reviews.getResponse().getContentAsString()).get(0).get("id").asLong();
  mvc.perform(put("/api/v1/task-reviews/"+review).header("Authorization",token("leader")).contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"approved\"}")).andExpect(status().isOk());
  mvc.perform(get("/api/v1/tasks/"+id).header("Authorization",token("staff"))).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("doing"));
 }
 @Test void invalidResponsiblePersonDoesNotPartiallyCreateTask()throws Exception {
  long staff=users.findByUsername("staff").orElseThrow().getId();var due=Instant.now().plusSeconds(86400).toString();
  long before=db.queryForObject("select count(*) from dispatch_tasks",Long.class);
  mvc.perform(post("/api/v1/tasks").header("Authorization",token("founder")).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("title","无效","assigneeId",staff,"assigneeIds",List.of(staff,999999L),"dueAt",due,"startAt",due)))).andExpect(status().isNotFound());
  assertThat(db.queryForObject("select count(*) from dispatch_tasks",Long.class)).isEqualTo(before);
 }
}
