package com.projectpandora.api;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.MediaType;
import java.time.*;
import java.util.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test") @Transactional
class TaskCreationDateTests {
 @Autowired org.springframework.test.web.servlet.MockMvc mvc;
 @Autowired com.fasterxml.jackson.databind.ObjectMapper json;
 @Autowired org.springframework.jdbc.core.JdbcTemplate db;
 @Test void creationHonorsShanghaiDateBoundaryForBothTimes() throws Exception {
  long id=db.queryForObject("select id from users where username='staff'",Long.class);
  var earliest=LocalDate.now(ZoneId.of("Asia/Shanghai")).minusDays(2).atStartOfDay(ZoneId.of("Asia/Shanghai")).toInstant();
  var auth=TaskPolicyTestSupport.token(mvc,json,"founder");
  for(boolean oldStart:List.of(false,true)) {
   var body=new HashMap<String,Object>();body.put("title","日期边界");body.put("assigneeId",id);
   body.put("dueAt",(oldStart?earliest.plusSeconds(3600):earliest.minusSeconds(1)).toString());
   if(oldStart)body.put("startAt",earliest.minusSeconds(1).toString());
   mvc.perform(post("/api/v1/tasks").header("Authorization",auth).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body))).andExpect(status().isBadRequest());
  }
  mvc.perform(post("/api/v1/tasks").header("Authorization",auth).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("title","允许前天","assigneeId",id,"dueAt",earliest.toString())))).andExpect(status().isCreated());
 }
}
