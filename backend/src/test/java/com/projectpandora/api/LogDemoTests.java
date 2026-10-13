package com.projectpandora.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class LogDemoTests {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;

    JsonNode login(String name) throws Exception {
        return json.readTree(mvc.perform(post("/api/v1/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsString(Map.of("username", name, "password", "demo1234"))))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
    }
    String token(JsonNode user) { return "Bearer " + user.get("token").asText(); }
    String body(String content, String state) throws Exception {
        return json.writeValueAsString(Map.of("logDate", LocalDate.now(java.time.ZoneId.of("Asia/Shanghai")).toString(), "content", content, "status", state));
    }
    JsonNode create(JsonNode user, String state) throws Exception {
        return json.readTree(mvc.perform(post("/api/v1/logs").header("Authorization",token(user))
            .contentType(MediaType.APPLICATION_JSON).content(body("原始记录",state)))
            .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());
    }
    @Test void draftSubmissionAndEditReachManagerWithSameId() throws Exception {
        var staff=login("staff"); var leader=login("leader"); var log=create(staff,"draft");
        long id=log.get("id").asLong(); String path="/api/v1/logs/"+id;
        assertThat(log.get("status").asText()).isEqualTo("draft");
        mvc.perform(get("/api/v1/logs").header("Authorization",token(staff)))
            .andExpect(jsonPath("$[0].id").value(id));
        mvc.perform(get("/api/v1/logs").param("userId",staff.get("userId").asText()).header("Authorization",token(leader)))
            .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
        for(int i=0;i<2;i++) mvc.perform(post(path+"/submit").header("Authorization",token(staff)))
            .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(id))
            .andExpect(jsonPath("$.status").value("submitted"));
        var edited=json.readTree(mvc.perform(put(path).header("Authorization",token(staff))
            .contentType(MediaType.APPLICATION_JSON).content(body("修改后的进度","submitted")))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        assertThat(edited.get("updatedAt").asText()).isNotEqualTo(log.get("updatedAt").asText());
        mvc.perform(get("/api/v1/logs").param("userId",staff.get("userId").asText())
            .param("date",LocalDate.now(java.time.ZoneId.of("Asia/Shanghai")).toString()).header("Authorization",token(leader)))
            .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].content").value("修改后的进度"));
    }
    @Test void panelUsesShanghaiDayEvenIfServerTimezoneDiffers() throws Exception {
        var staff=login("staff");
        var original=java.util.TimeZone.getDefault();
        var shanghai=LocalDate.now(java.time.ZoneId.of("Asia/Shanghai"));
        var other=java.time.ZoneId.of("Etc/GMT+12");
        if(LocalDate.now(other).equals(shanghai)) other=java.time.ZoneId.of("Etc/GMT-14");
        try {
            java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone(other));
            // Model a server started in another timezone; do not change it halfway through storing a DATE.
            var log=create(staff,"submitted");
            mvc.perform(get("/api/v1/panels/map").header("Authorization",token(staff)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.todayLogs[0].id").value(log.get("id").asLong()));
        } finally {java.util.TimeZone.setDefault(original);}
    }
    @Test void omittedStatusRemainsSubmitted() throws Exception {
        var staff=login("staff");
        mvc.perform(post("/api/v1/logs").header("Authorization",token(staff))
            .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("logDate",LocalDate.now(java.time.ZoneId.of("Asia/Shanghai")).toString(),"content","兼容客户端"))))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.status").value("submitted"));
    }
    @Test void permissionsAndPublicMemberScope() throws Exception {
        var staff=login("staff"); var leader=login("leader"); var admin=login("admin");
        var log=create(staff,"submitted"); String path="/api/v1/logs/"+log.get("id");
        mvc.perform(put(path).header("Authorization",token(leader)).contentType(MediaType.APPLICATION_JSON).content(body("越权","submitted")))
            .andExpect(status().isForbidden());
        mvc.perform(post(path+"/submit").header("Authorization",token(admin))).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/logs").param("userId",leader.get("userId").asText()).header("Authorization",token(staff)))
            .andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/logs").param("userId",admin.get("userId").asText()).header("Authorization",token(leader)))
            .andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/users").header("Authorization",token(staff)))
            .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].id").value(staff.get("userId").asLong()))
            .andExpect(jsonPath("$[0].passwordHash").doesNotExist());
        var members=json.readTree(mvc.perform(get("/api/v1/users").header("Authorization",token(leader)))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        assertThat(members.size()).isEqualTo(2);
        assertThat(members.toString()).doesNotContain("password", "ADMIN");
        mvc.perform(get("/api/v1/users").header("Authorization",token(admin)))
            .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(3));
        mvc.perform(get("/api/v1/users")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/logs").header("Authorization","Bearer broken")).andExpect(status().isUnauthorized());
    }
    @Test void invalidInputPreservesRecordAndCannotDowngrade() throws Exception {
        var staff=login("staff"); var log=create(staff,"submitted"); String path="/api/v1/logs/"+log.get("id");
        for(String content : new String[]{"   ","x".repeat(5001)})
            mvc.perform(put(path).header("Authorization",token(staff)).contentType(MediaType.APPLICATION_JSON).content(body(content,"submitted")))
                .andExpect(status().isBadRequest());
        for(String state : new String[]{"draft","unknown"})
            mvc.perform(put(path).header("Authorization",token(staff)).contentType(MediaType.APPLICATION_JSON).content(body("被拒绝",state)))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/v1/logs").header("Authorization",token(staff)).contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsString(Map.of("logDate",LocalDate.now(java.time.ZoneId.of("Asia/Shanghai")).plusDays(2).toString(),"content","未来"))))
            .andExpect(status().isBadRequest());
        mvc.perform(post("/api/v1/logs").header("Authorization",token(staff)).contentType(MediaType.APPLICATION_JSON).content("{bad"))
            .andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/logs").param("date","bad").header("Authorization",token(staff)))
            .andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/logs").header("Authorization",token(staff)))
            .andExpect(jsonPath("$[0].content").value("原始记录"));
    }
}
