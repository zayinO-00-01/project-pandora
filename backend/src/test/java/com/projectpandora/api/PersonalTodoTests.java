package com.projectpandora.api;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.MediaType;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test") @Transactional
class PersonalTodoTests {
 @Autowired org.springframework.test.web.servlet.MockMvc mvc;
 @Autowired com.fasterxml.jackson.databind.ObjectMapper json;
 @Test void privateDailyTodosCanBeEditedCompletedAndDeletedButNeverReadOrChangedByOthers() throws Exception {
  String staff=TaskPolicyTestSupport.token(mvc,json,"staff"),founder=TaskPolicyTestSupport.token(mvc,json,"founder");
  String input="{\"title\":\"个人买书\",\"date\":\"2026-11-05\",\"completed\":false}";
  var result=mvc.perform(post("/api/v1/personal-todos").header("Authorization",staff).contentType(MediaType.APPLICATION_JSON).content(input)).andExpect(status().isCreated()).andReturn();
  String id=json.readTree(result.getResponse().getContentAsString()).get("id").asText();
  mvc.perform(get("/api/v1/personal-todos").header("Authorization",staff)).andExpect(jsonPath("$[0].title").value("个人买书"));
  mvc.perform(get("/api/v1/personal-todos").header("Authorization",founder)).andExpect(jsonPath("$.length()").value(0));
  mvc.perform(put("/api/v1/personal-todos/"+id).header("Authorization",founder).contentType(MediaType.APPLICATION_JSON).content(input)).andExpect(status().isNotFound());
  mvc.perform(delete("/api/v1/personal-todos/"+id).header("Authorization",founder)).andExpect(status().isNotFound());
  mvc.perform(put("/api/v1/personal-todos/"+id).header("Authorization",staff).contentType(MediaType.APPLICATION_JSON).content(input.replace("false","true"))).andExpect(status().isOk()).andExpect(jsonPath("$.completed").value(true));
  mvc.perform(delete("/api/v1/personal-todos/"+id).header("Authorization",staff)).andExpect(status().isOk());
  mvc.perform(get("/api/v1/personal-todos").header("Authorization",staff)).andExpect(jsonPath("$.length()").value(0));
 }
 @Autowired org.springframework.jdbc.core.JdbcTemplate db;
 @Test void sharedLabelsPersistAcrossDateMovesAndCannotBeChosenAfterArchive() throws Exception {
  String staff=TaskPolicyTestSupport.token(mvc,json,"staff");
  var ids=db.queryForList("select id from work_labels where name in ('计划','跟进') order by id",Long.class);
  org.junit.jupiter.api.Assertions.assertEquals(2,ids.size());
  var body=json.createObjectNode().put("title","跟进项目").put("date","2026-11-14");
  body.putArray("labelIds").add(ids.get(1)).add(ids.get(0));
  var result=mvc.perform(post("/api/v1/personal-todos").header("Authorization",staff).contentType(MediaType.APPLICATION_JSON).content(body.toString()))
      .andExpect(status().isCreated()).andExpect(jsonPath("$.labels.length()").value(2)).andReturn();
  String id=json.readTree(result.getResponse().getContentAsString()).get("id").asText();
  var founder=TaskPolicyTestSupport.token(mvc,json,"founder");
  mvc.perform(delete("/api/v1/labels/"+ids.get(1)).header("Authorization",staff)).andExpect(status().isForbidden());
  mvc.perform(delete("/api/v1/labels/"+ids.get(1)).header("Authorization",founder)).andExpect(status().isOk());
  mvc.perform(post("/api/v1/personal-todos").header("Authorization",staff).contentType(MediaType.APPLICATION_JSON).content(body.toString()))
      .andExpect(status().isBadRequest());
  body.remove("labelIds");body.put("date","2026-11-16");
  mvc.perform(put("/api/v1/personal-todos/"+id).header("Authorization",staff).contentType(MediaType.APPLICATION_JSON).content(body.toString()))
      .andExpect(status().isOk()).andExpect(jsonPath("$.date").value("2026-11-16"))
      .andExpect(jsonPath("$.labels[0].active").value(false)).andExpect(jsonPath("$.labels.length()").value(2));
 }
 @Test void invalidTodoAndAnonymousRequestAreRejected() throws Exception {
  mvc.perform(get("/api/v1/personal-todos")).andExpect(status().isUnauthorized());
  mvc.perform(post("/api/v1/personal-todos").header("Authorization",TaskPolicyTestSupport.token(mvc,json,"staff")).contentType(MediaType.APPLICATION_JSON).content("{\"title\":\" \"}")).andExpect(status().isBadRequest());
 }
 @Test void repeatedCreationReturnsTheSameItemWithoutDuplicatingIt() throws Exception {
  String staff=TaskPolicyTestSupport.token(mvc,json,"staff");
  String input="{\"title\":\"买书\",\"date\":\"2026-11-19\",\"requestId\":\"7b0b60dd-ab88-4de3-9671-4f15d7a582ae\"}";
  var first=mvc.perform(post("/api/v1/personal-todos").header("Authorization",staff).contentType(MediaType.APPLICATION_JSON).content(input)).andExpect(status().isCreated()).andReturn();
  String id=json.readTree(first.getResponse().getContentAsString()).get("id").asText();
  mvc.perform(post("/api/v1/personal-todos").header("Authorization",staff).contentType(MediaType.APPLICATION_JSON).content(input))
      .andExpect(status().isCreated()).andExpect(jsonPath("$.id").value(id));
  mvc.perform(get("/api/v1/personal-todos").header("Authorization",staff)).andExpect(jsonPath("$.length()").value(1));
 }
 @Test void oldSnapshotMergesIndependentFieldsButRejectsTwoChangesToTheSameField() throws Exception {
  String staff=TaskPolicyTestSupport.token(mvc,json,"staff");
  String base="{\"title\":\"买书\",\"date\":\"2026-11-19\",\"completed\":false,\"labelIds\":[]}";
  var result=mvc.perform(post("/api/v1/personal-todos").header("Authorization",staff).contentType(MediaType.APPLICATION_JSON).content(base)).andExpect(status().isCreated()).andReturn();
  String path="/api/v1/personal-todos/"+json.readTree(result.getResponse().getContentAsString()).get("id").asText();
  String rename="{\"title\":\"买工作用书\",\"date\":\"2026-11-19\",\"completed\":false,\"base\":"+base+"}";
  String complete="{\"title\":\"买书\",\"date\":\"2026-11-19\",\"completed\":true,\"base\":"+base+"}";
  mvc.perform(put(path).header("Authorization",staff).contentType(MediaType.APPLICATION_JSON).content(rename)).andExpect(status().isOk());
  mvc.perform(put(path).header("Authorization",staff).contentType(MediaType.APPLICATION_JSON).content(complete)).andExpect(status().isOk())
      .andExpect(jsonPath("$.title").value("买工作用书")).andExpect(jsonPath("$.completed").value(true));
  mvc.perform(put(path).header("Authorization",staff).contentType(MediaType.APPLICATION_JSON).content(rename.replace("买工作用书","买别的书")))
      .andExpect(status().isConflict());
  mvc.perform(get("/api/v1/personal-todos").header("Authorization",staff)).andExpect(jsonPath("$[0].title").value("买工作用书"));
 }
}
