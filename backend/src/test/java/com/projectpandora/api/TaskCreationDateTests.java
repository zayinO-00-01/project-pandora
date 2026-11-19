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
 @Test void deadlineCannotBeBeforeTodayButStartMayBeHistorical() throws Exception {
  long id=db.queryForObject("select id from users where username='staff'",Long.class);
  var zone=ZoneId.of("Asia/Shanghai"); var today=LocalDate.now(zone);
  var auth=TaskPolicyTestSupport.token(mvc,json,"founder");
  var past=new HashMap<String,Object>();past.put("title","过期截止");past.put("assigneeId",id);past.put("dueAt",today.minusDays(1).atTime(10,0).atZone(zone).toInstant().toString());
  mvc.perform(post("/api/v1/tasks").header("Authorization",auth).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(past))).andExpect(status().isBadRequest());
  var valid=new HashMap<String,Object>();valid.put("title","历史开始");valid.put("assigneeId",id);valid.put("startAt",today.minusDays(3).atTime(10,0).atZone(zone).toInstant().toString());valid.put("dueAt",today.plusDays(1).atTime(10,0).atZone(zone).toInstant().toString());
  mvc.perform(post("/api/v1/tasks").header("Authorization",auth).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(valid))).andExpect(status().isCreated());
  var invalid=new HashMap<String,Object>();invalid.put("title","开始晚于截止");invalid.put("assigneeId",id);invalid.put("startAt",today.plusDays(3).atTime(10,0).atZone(zone).toInstant().toString());invalid.put("dueAt",today.plusDays(1).atTime(10,0).atZone(zone).toInstant().toString());
  mvc.perform(post("/api/v1/tasks").header("Authorization",auth).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(invalid))).andExpect(status().isBadRequest());
 }}
