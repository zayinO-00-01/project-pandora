package com.projectpandora.api;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.MediaType;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test") @Transactional
class ContentLabelsTests {
@Autowired org.springframework.test.web.servlet.MockMvc mvc;
@Autowired com.fasterxml.jackson.databind.ObjectMapper json;
@Autowired org.springframework.jdbc.core.JdbcTemplate db;
String token(String name)throws Exception{return TaskPolicyTestSupport.token(mvc,json,name);}
@Test void multiLabelsPersistAndOtherAuthorsCannotChangeThem()throws Exception {
 long owner=db.queryForObject("select id from users where username='staff'",Long.class);
 db.update("insert into work_logs(user_id,log_date,end_date,content,status,created_at,updated_at) values(?,CURRENT_DATE,CURRENT_DATE,'test','submitted',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)",owner);
 long id=db.queryForObject("select max(id) from work_logs",Long.class);
 db.update("insert into work_labels(name,color,active) values('A','#ABCDEF',true),('B','#123456',true)");
 var ids=db.queryForList("select id from work_labels where name in ('A','B') order by id",Long.class);
 String body=json.writeValueAsString(java.util.Map.of("labelIds",ids));
 mvc.perform(put("/api/v1/content-labels/log/"+id).header("Authorization",token("staff")).contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isOk()).andExpect(jsonPath("$.labels.length()").value(2));
 mvc.perform(get("/api/v1/content-labels").header("Authorization",token("staff"))).andExpect(status().isOk()).andExpect(jsonPath("$[0].labels.length()").value(2));
 mvc.perform(put("/api/v1/content-labels/log/"+id).header("Authorization",token("team_lead")).contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isForbidden());
 mvc.perform(put("/api/v1/content-labels/log/"+id).header("Authorization",token("staff")).contentType(MediaType.APPLICATION_JSON).content("{\"labelIds\":[]}")).andExpect(status().isOk()).andExpect(jsonPath("$.labels.length()").value(0));
 mvc.perform(get("/api/v1/content-labels")).andExpect(status().isUnauthorized());
}
@Test void newLogAndTaskSaveMultipleLabelsInTheirCreateTransaction()throws Exception {
 db.update("insert into work_labels(name,color,active) values('CreateA','#ABCDEF',true),('CreateB','#123456',true)");
 var ids=db.queryForList("select id from work_labels where name in ('CreateA','CreateB') order by id",Long.class);
 long owner=db.queryForObject("select id from users where username='founder'",Long.class);
 String body=json.writeValueAsString(java.util.Map.of("title","multi","assigneeId",owner,"dueAt","2026-12-01T10:00:00Z","labelIds",ids));
 mvc.perform(post("/api/v1/tasks").header("Authorization",token("founder")).contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isCreated());
 body=json.writeValueAsString(java.util.Map.of("logDate",java.time.LocalDate.now(java.time.ZoneId.of("Asia/Shanghai")).toString(),"content","multi","labelIds",ids));
 mvc.perform(post("/api/v1/logs").header("Authorization",token("founder")).contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isCreated());
 mvc.perform(get("/api/v1/content-labels").header("Authorization",token("founder"))).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2)).andExpect(jsonPath("$[0].labels.length()").value(2)).andExpect(jsonPath("$[1].labels.length()").value(2));
}
}
