package com.projectpandora.api;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.MediaType;
import java.util.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test") @Transactional
class BatchTaskTests {
 @Autowired org.springframework.test.web.servlet.MockMvc mvc;@Autowired com.fasterxml.jackson.databind.ObjectMapper json;@Autowired org.springframework.jdbc.core.JdbcTemplate db;
 long uid(String name){return db.queryForObject("select id from users where username=?",Long.class,name);}
 Map<String,Object> task(long id){return Map.of("title","批量安排","assigneeId",id,"dueAt","2026-12-01T10:00:00Z");}
 @Test void createsDistinctTasksAndEachKeepsItsOwnProgressAndApproval()throws Exception {
 var body=json.writeValueAsString(Map.of("tasks",List.of(task(uid("staff")),task(uid("team_lead")))));
 var result=mvc.perform(post("/api/v1/tasks/batch").header("Authorization",TaskPolicyTestSupport.token(mvc,json,"staff")).contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isCreated()).andExpect(jsonPath("$.length()").value(2)).andReturn();
 var rows=json.readTree(result.getResponse().getContentAsString());assertThat(rows.get(0).get("id")).isNotEqualTo(rows.get(1).get("id"));for(var row:rows)assertThat(row.get("approvalStatus").asText()).isEqualTo("pending");
 }
 @Test @Transactional(propagation=org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED) void invalidLastAssigneeRollsBackWholeBatch()throws Exception {
 long before=db.queryForObject("select count(*) from dispatch_tasks",Long.class);
 var body=json.writeValueAsString(Map.of("tasks",List.of(task(uid("staff")),task(9999999))));
 mvc.perform(post("/api/v1/tasks/batch").header("Authorization",TaskPolicyTestSupport.token(mvc,json,"founder")).contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isNotFound());
 assertThat(db.queryForObject("select count(*) from dispatch_tasks",Long.class)).isEqualTo(before);
 }
 @Test void rejectsDuplicatesEmptyAndMissingDeadline()throws Exception {
 for(var tasks:List.of(List.of(task(uid("staff")),task(uid("staff"))),List.of(),List.of(Map.of("title","invalid","assigneeId",uid("staff")))))
 mvc.perform(post("/api/v1/tasks/batch").header("Authorization",TaskPolicyTestSupport.token(mvc,json,"founder")).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("tasks",tasks)))).andExpect(status().isBadRequest());
 }
}
