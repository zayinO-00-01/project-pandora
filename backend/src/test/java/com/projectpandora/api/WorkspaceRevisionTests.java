package com.projectpandora.api;
import com.fasterxml.jackson.databind.*;
import com.projectpandora.api.user.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.MediaType;
import java.time.*;
import java.util.Map;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test") @Transactional
class WorkspaceRevisionTests {
    @Autowired org.springframework.test.web.servlet.MockMvc mvc; @Autowired ObjectMapper json; @Autowired UserRepository users;
    String token(String name) throws Exception {return TaskPolicyTestSupport.token(mvc,json,name);}
    @Test void dailyLogDraftAndSubmittedShareOneSlotAndAuthorsAreIndependent() throws Exception {
        var date=LocalDate.now(ZoneId.of("Asia/Shanghai")).toString();
        var body=json.writeValueAsString(Map.of("logDate",date,"content","日报","status","draft"));
        var first=mvc.perform(post("/api/v1/logs").header("Authorization",token("staff")).contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isCreated()).andReturn();
        long id=json.readTree(first.getResponse().getContentAsString()).get("id").asLong();
        for(String state:java.util.List.of("draft","submitted"))
            mvc.perform(post("/api/v1/logs").header("Authorization",token("staff")).contentType(MediaType.APPLICATION_JSON).content(body.replace("draft",state))).andExpect(status().isConflict());
        mvc.perform(post("/api/v1/logs/"+id+"/submit").header("Authorization",token("staff"))).andExpect(status().isOk());
        mvc.perform(post("/api/v1/logs").header("Authorization",token("team_lead")).contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isCreated());
    }
    @Test void staffCannotOpenReviewerListsButCanReadOwnNotifications() throws Exception {
        for(String path:java.util.List.of("/tasks/pending-approvals","/tasks/reviews")) {
            mvc.perform(get("/api/v1"+path).header("Authorization",token("staff"))).andExpect(status().isForbidden());
            mvc.perform(get("/api/v1"+path).header("Authorization",token("team_lead"))).andExpect(status().isOk());
        }
        mvc.perform(get("/api/v1/notifications").header("Authorization",token("staff"))).andExpect(status().isOk());
    }
    @Test void directoryIsCompanyWideButContainsNoPrivatePersonnelFields() throws Exception {
        var staff=users.findByUsername("staff").orElseThrow();staff.setPhone("13800001234");users.saveAndFlush(staff);
        var response=mvc.perform(get("/api/v1/directory").header("Authorization",token("staff"))).andExpect(status().isOk()).andReturn();
        var contacts=json.readTree(response.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8));
        assertThat(contacts.findValuesAsText("username")).contains("staff","team_lead","leader","founder").doesNotContain("admin");
        assertThat(contacts.toString()).doesNotContain("passwordHash","idCard","birthDate","emergencyPhone","address");
        assertThat(contacts.findValuesAsText("phone")).contains("13800001234");
        mvc.perform(get("/api/v1/directory")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/directory").header("Authorization",token("admin"))).andExpect(status().isForbidden());
        staff.setDisabled(true);users.saveAndFlush(staff);
        var list=mvc.perform(get("/api/v1/directory").header("Authorization",token("founder"))).andExpect(status().isOk()).andReturn();
        assertThat(json.readTree(list.getResponse().getContentAsString()).findValuesAsText("username")).doesNotContain("staff");
    }
}
