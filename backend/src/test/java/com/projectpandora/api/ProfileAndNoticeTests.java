package com.projectpandora.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
class ProfileAndNoticeTests {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;

    JsonNode login(String name) throws Exception {
        return json.readTree(mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsString(Map.of("username", name, "password", "demo1234"))))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8));
    }
    String token(JsonNode user) { return "Bearer " + user.get("token").asText(); }

    String createBody(String username, String phone) throws Exception {
        return json.writeValueAsString(Map.of("username", username, "password", "demo1234", "role", "STAFF", "phone", phone));
    }

    @Test void ownerCanChangePasswordWithCurrentPassword() throws Exception {
        var staff=login("staff");var auth=token(staff);
        mvc.perform(put("/api/v1/users/me/password").header("Authorization",auth).contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsString(Map.of("currentPassword","wrong","newPassword","newpass123"))))
            .andExpect(status().isBadRequest());
        mvc.perform(put("/api/v1/users/me/password").header("Authorization",auth).contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsString(Map.of("currentPassword","demo1234","newPassword","newpass123"))))
            .andExpect(status().isOk());
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsString(Map.of("username","staff","password","newpass123"))))
            .andExpect(status().isOk());
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsString(Map.of("username","staff","password","demo1234"))))
            .andExpect(status().isUnauthorized());
    }

    @Test void ownerCanChangeAvatarAndReadBack() throws Exception {
        var staff=login("staff");var auth=token(staff);
        mvc.perform(put("/api/v1/users/me/avatar").header("Authorization",auth).contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsString(Map.of("avatar","FOX"))))
            .andExpect(status().isOk()).andExpect(jsonPath("$.avatar").value("FOX"));
        mvc.perform(get("/api/v1/users/me").header("Authorization",auth))
            .andExpect(status().isOk()).andExpect(jsonPath("$.avatar").value("FOX"))
            .andExpect(jsonPath("$.managerId").isNumber());
    }

    @Test void profilePhoneIsRequiredUniqueAndValidated() throws Exception {
        var admin = login("admin");
        // missing phone rejected
        mvc.perform(post("/api/v1/admin/users").header("Authorization", token(admin)).contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsString(Map.of("username", "no_phone_user", "password", "demo1234", "role", "STAFF"))))
            .andExpect(status().isBadRequest());
        // malformed phone rejected
        mvc.perform(post("/api/v1/admin/users").header("Authorization", token(admin)).contentType(MediaType.APPLICATION_JSON)
            .content(createBody("bad_phone_user", "12345"))).andExpect(status().isBadRequest());
        // valid creation
        mvc.perform(post("/api/v1/admin/users").header("Authorization", token(admin)).contentType(MediaType.APPLICATION_JSON)
            .content(createBody("good_phone_user", "13800001234")))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.phone").value("13800001234"));
        // duplicate phone rejected
        mvc.perform(post("/api/v1/admin/users").header("Authorization", token(admin)).contentType(MediaType.APPLICATION_JSON)
            .content(createBody("dup_phone_user", "13800001234"))).andExpect(status().isBadRequest());
    }

    @Test void idCardIsValidatedAndRejectsFutureBirthDate() throws Exception {
        var admin = login("admin");
        mvc.perform(post("/api/v1/admin/users").header("Authorization", token(admin)).contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsString(Map.of("username", "bad_id_user", "password", "demo1234", "role", "STAFF",
                "phone", "13800002222", "idCard", "123")))).andExpect(status().isBadRequest());
        mvc.perform(post("/api/v1/admin/users").header("Authorization", token(admin)).contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsString(Map.of("username", "future_birth_user", "password", "demo1234", "role", "STAFF",
                "phone", "13800003333", "birthDate", "2999-01-01")))).andExpect(status().isBadRequest());
        mvc.perform(post("/api/v1/admin/users").header("Authorization", token(admin)).contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsString(Map.of("username", "good_profile_user", "password", "demo1234", "role", "STAFF",
                "phone", "13800004444", "idCard", "11010519491231002X", "birthDate", "1990-05-01", "gender", "FEMALE"))))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.idCard").value("11010519491231002X"))
            .andExpect(jsonPath("$.gender").value("FEMALE"));
    }

    @Test void ownerReadsOwnFullProfile() throws Exception {
        var staff = login("staff");
        mvc.perform(get("/api/v1/users/me").header("Authorization", token(staff)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.username").value("staff"))
            .andExpect(jsonPath("$.role").value("STAFF"));
        mvc.perform(get("/api/v1/users/me")).andExpect(status().isUnauthorized());
    }

    @Test void adminPublishesCompanyNoticeAndEveryoneReads() throws Exception {
        var admin = login("admin");
        mvc.perform(post("/api/v1/admin/company-notices").header("Authorization", token(admin)).contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsString(Map.of("title", "安全巡查", "content", "本月重点", "sortOrder", 1, "enabled", true))))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.title").value("安全巡查"));
        var staff = login("staff");
        mvc.perform(get("/api/v1/company-notices").header("Authorization", token(staff)))
            .andExpect(status().isOk()).andExpect(jsonPath("$[0].title").value("安全巡查"));
        // staff cannot publish
        mvc.perform(post("/api/v1/admin/company-notices").header("Authorization", token(staff)).contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsString(Map.of("title", "越权", "content", "不应创建")))).andExpect(status().isForbidden());
        // panel shows the notice
        mvc.perform(get("/api/v1/panels/map").header("Authorization", token(staff)))
            .andExpect(status().isOk()).andExpect(jsonPath("$.companyImportant[0].title").value("安全巡查"));
    }

    @Test void elevelthEnabledNoticeIsRejected() throws Exception {
        var admin = login("admin");
        for (int i = 1; i <= 10; i++) {
            mvc.perform(post("/api/v1/admin/company-notices").header("Authorization", token(admin)).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("title", "事项" + i, "content", "内容" + i, "enabled", true))))
                .andExpect(status().isCreated());
        }
        mvc.perform(post("/api/v1/admin/company-notices").header("Authorization", token(admin)).contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsString(Map.of("title", "第十一", "content", "超限", "enabled", true))))
            .andExpect(status().isBadRequest());
    }

    @Test void personalNoteIsWrittenAndKeptPrivate() throws Exception {
        var admin = login("admin");
        var created = json.readTree(mvc.perform(post("/api/v1/admin/company-notices").header("Authorization", token(admin))
            .contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsString(Map.of("title", "备注事项", "content", "正文", "enabled", true))))
            .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8));
        long id = created.get("id").asLong();
        var staff = login("staff");
        mvc.perform(put("/api/v1/company-notices/" + id + "/note").header("Authorization", token(staff))
            .contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsString(Map.of("content", "我的理解"))))
            .andExpect(status().isOk()).andExpect(jsonPath("$.myNote").value("我的理解"));
        mvc.perform(get("/api/v1/company-notices/" + id).header("Authorization", token(staff)))
            .andExpect(status().isOk()).andExpect(jsonPath("$.myNote").value("我的理解"));
        var other = login("team_lead");
        mvc.perform(get("/api/v1/company-notices/" + id).header("Authorization", token(other)))
            .andExpect(status().isOk()).andExpect(jsonPath("$.myNote").value(org.hamcrest.Matchers.nullValue()));
        assertThat(created.has("myNote")).isFalse();
    }
}
