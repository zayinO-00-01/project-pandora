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
 @Test void invalidTodoAndAnonymousRequestAreRejected() throws Exception {
  mvc.perform(get("/api/v1/personal-todos")).andExpect(status().isUnauthorized());
  mvc.perform(post("/api/v1/personal-todos").header("Authorization",TaskPolicyTestSupport.token(mvc,json,"staff")).contentType(MediaType.APPLICATION_JSON).content("{\"title\":\" \"}")).andExpect(status().isBadRequest());
 }
}
