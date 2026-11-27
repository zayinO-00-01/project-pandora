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
    @Autowired org.springframework.jdbc.core.JdbcTemplate db;

    JsonNode login(String name) throws Exception {
        return json.readTree(mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsString(Map.of("username", name, "password", "demo1234"))))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8));
    }
    String token(JsonNode user) { return "Bearer " + user.get("token").asText(); }

    com.fasterxml.jackson.databind.node.ObjectNode staffAccount(String username, String phone) {
        return json.createObjectNode().put("username", username).put("password", "demo1234").put("role", "STAFF")
            .put("displayName", "示例姓名").put("phone", phone).put("idCard", TestIdentity.card(410))
            .put("managerId", db.queryForObject("select id from users where username='team_lead'", Long.class));
    }

    @Test void adminAccountUsesTheRoleTitleInsteadOfAPersonalName() throws Exception {
        var auth=token(login("admin"));
        var input=staffAccount("sys_admin", "13800003600").put("role", "ADMIN");
        input.remove(java.util.List.of("displayName", "managerId"));
        var created=mvc.perform(post("/api/v1/admin/users").header("Authorization",auth).contentType(MediaType.APPLICATION_JSON)
            .content(input.toString())).andExpect(status().isCreated())
            .andExpect(jsonPath("$.displayName").value("管理员")).andReturn();
        long id=json.readTree(created.getResponse().getContentAsString()).path("id").asLong();
        mvc.perform(patch("/api/v1/admin/users/"+id).header("Authorization",auth).contentType(MediaType.APPLICATION_JSON)
            .content(json.createObjectNode().put("displayName", "另一个名字").toString()))
            .andExpect(status().isOk()).andExpect(jsonPath("$.displayName").value("管理员"));
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
            .andExpect(status().isOk()).andExpect(jsonPath("$.avatar").value("FOX"))
            .andExpect(jsonPath("$.avatarImage").doesNotExist());
        mvc.perform(get("/api/v1/users/me").header("Authorization",auth))
            .andExpect(status().isOk()).andExpect(jsonPath("$.avatar").value("FOX"))
            .andExpect(jsonPath("$.managerId").isNumber());
        String image="data:image/png;base64,AAAA";
        mvc.perform(put("/api/v1/users/me/avatar").header("Authorization",auth).contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsString(Map.of("avatar",image))))
            .andExpect(status().isOk()).andExpect(jsonPath("$.avatarImage").value(image))
            .andExpect(jsonPath("$.avatar").value(org.hamcrest.Matchers.nullValue()));
        mvc.perform(put("/api/v1/users/me/avatar").header("Authorization",auth).contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsString(Map.of("avatar","data:text/plain;base64,AAAA"))))
            .andExpect(status().isBadRequest());
    }

    @Test void profilePhoneIsRequiredUniqueAndValidated() throws Exception {
        var admin = login("admin");
        // missing phone rejected
        mvc.perform(post("/api/v1/admin/users").header("Authorization", token(admin)).contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsString(Map.of("username", "no_phone_user", "password", "demo1234", "role", "STAFF"))))
            .andExpect(status().isBadRequest());
        // malformed phone rejected
        mvc.perform(post("/api/v1/admin/users").header("Authorization", token(admin)).contentType(MediaType.APPLICATION_JSON)
            .content(staffAccount("bad_phone_user", "12345").toString())).andExpect(status().isBadRequest());
        // valid creation
        mvc.perform(post("/api/v1/admin/users").header("Authorization", token(admin)).contentType(MediaType.APPLICATION_JSON)
            .content(staffAccount("good_phone_user", "13800001234").toString()))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.phone").value("13800001234"));
        // duplicate phone rejected
        mvc.perform(post("/api/v1/admin/users").header("Authorization", token(admin)).contentType(MediaType.APPLICATION_JSON)
            .content(staffAccount("dup_phone_user", "13800001234").put("idCard", TestIdentity.card(411)).toString())).andExpect(status().isBadRequest());
    }

    @Test void adminEnforcesNameAndUsernameRulesEvenForDisabledAccounts() throws Exception {
        String admin = token(login("admin"));
        String users = "/api/v1/admin/users";
        var first = staffAccount("Roster_One", "13700003410").put("displayName", "Lin Ming")
            .put("idCard", TestIdentity.card(414)).put("disabled", true);
        var second = staffAccount("Roster_Two", "13700003411").put("displayName", "欧阳明远")
            .put("idCard", TestIdentity.card(415));
        for (var invalid : java.util.List.of(second.deepCopy().put("username", "Roster-One"),
                second.deepCopy().put("displayName", "陆明!"))) {
            mvc.perform(post(users).header("Authorization", admin).contentType(MediaType.APPLICATION_JSON)
                .content(invalid.toString())).andExpect(status().isBadRequest());
        }
        mvc.perform(post(users).header("Authorization", admin).contentType(MediaType.APPLICATION_JSON)
            .content(first.toString())).andExpect(status().isCreated());
        for (var duplicate : java.util.List.of(second.deepCopy().put("username", "roster_one"),
                second.deepCopy().put("displayName", "lin ming"), second.deepCopy().put("phone", "13700003410"),
                second.deepCopy().put("idCard", TestIdentity.card(414)))) {
            mvc.perform(post(users).header("Authorization", admin).contentType(MediaType.APPLICATION_JSON)
                .content(duplicate.toString())).andExpect(status().isBadRequest());
        }
        var response = mvc.perform(post(users).header("Authorization", admin).contentType(MediaType.APPLICATION_JSON)
            .content(second.toString())).andExpect(status().isCreated()).andReturn().getResponse();
        String path = users + "/" + json.readTree(response.getContentAsByteArray()).path("id").asLong();
        mvc.perform(patch(path).header("Authorization", admin).contentType(MediaType.APPLICATION_JSON)
            .content(json.createObjectNode().put("displayName", "Lin Ming").toString())).andExpect(status().isBadRequest());
        mvc.perform(patch(path).header("Authorization", admin).contentType(MediaType.APPLICATION_JSON)
            .content(json.createObjectNode().put("displayName", "欧阳明远").toString())).andExpect(status().isOk());
        mvc.perform(patch(path).header("Authorization", admin).contentType(MediaType.APPLICATION_JSON)
            .content("{\"disabled\":true}")).andExpect(status().isOk()).andExpect(jsonPath("$.disabled").value(true));
        mvc.perform(patch(path).header("Authorization", admin).contentType(MediaType.APPLICATION_JSON)
            .content("{\"disabled\":false}")).andExpect(status().isOk()).andExpect(jsonPath("$.disabled").value(false));
    }

    @Test void adminPatchDistinguishesOmittedDatesFromExplicitlyClearedDates() throws Exception {
        String admin=token(login("admin"));
        db.update("update users set id_card=null, birth_date=DATE '1990-05-01', hire_date=DATE '2025-01-01' where username='staff'");
        String path="/api/v1/admin/users/"+login("staff").path("userId").asLong();
        mvc.perform(patch(path).header("Authorization",admin).contentType(MediaType.APPLICATION_JSON)
            .content("{\"displayName\":\"New title\"}")).andExpect(status().isOk())
            .andExpect(jsonPath("$.birthDate").value("1990-05-01")).andExpect(jsonPath("$.hireDate").value("2025-01-01"));
        mvc.perform(patch(path).header("Authorization",admin).contentType(MediaType.APPLICATION_JSON)
            .content("{\"birthDate\":null,\"hireDate\":null}")).andExpect(status().isOk())
            .andExpect(jsonPath("$.birthDate").isEmpty()).andExpect(jsonPath("$.hireDate").isEmpty());
    }

    @Test void adminCanEditAccountWithUnchangedLegacyContactPhone() throws Exception {
        db.update("update users set phone='010-12345678' where username='staff'");
        var staff=login("staff");
        String own=token(staff);
        mvc.perform(put("/api/v1/users/me").header("Authorization",token(staff)).contentType(MediaType.APPLICATION_JSON)
            .content("{\"phone\":\"010-12345678\"}")).andExpect(status().isOk())
            .andExpect(jsonPath("$.phone").value("010-12345678"));
        mvc.perform(put("/api/v1/users/me").header("Authorization",own).contentType(MediaType.APPLICATION_JSON)
            .content("{\"address\":\"Personal edit\"}")).andExpect(status().isOk())
            .andExpect(jsonPath("$.phone").value("010-12345678"));
        String path="/api/v1/admin/users/"+staff.get("userId").asLong();
        String admin=token(login("admin"));
        mvc.perform(patch(path).header("Authorization",admin).contentType(MediaType.APPLICATION_JSON)
            .content("{\"displayName\":\"Revised\"}")).andExpect(status().isOk())
            .andExpect(jsonPath("$.phone").value("010-12345678")).andExpect(jsonPath("$.displayName").value("Revised"));
        mvc.perform(patch(path).header("Authorization",admin).contentType(MediaType.APPLICATION_JSON)
            .content("{\"phone\":\"010-12345679\"}")).andExpect(status().isBadRequest());
        mvc.perform(patch(path).header("Authorization",admin).contentType(MediaType.APPLICATION_JSON)
            .content("{\"phone\":\"13700001234\"}")).andExpect(status().isOk())
            .andExpect(jsonPath("$.phone").value("13700001234"));
    }

    @Test void requiredProfileFieldsAndDerivedBirthdayAreEnforced() throws Exception {
        var admin = login("admin");
        var input = staffAccount("good_profile_user", "13800004444");
        for (String field : new String[]{"displayName", "idCard", "managerId"}) {
            var incomplete = input.deepCopy();
            incomplete.remove(field);
            mvc.perform(post("/api/v1/admin/users").header("Authorization", token(admin)).contentType(MediaType.APPLICATION_JSON)
                .content(incomplete.toString())).andExpect(status().isBadRequest());
        }
        for (String invalid : new String[]{"130503900101001", "11010519491231002Y", "110105194912310021",
                TestIdentity.card("29990101", 413), TestIdentity.card("19900230", 413)}) {
            mvc.perform(post("/api/v1/admin/users").header("Authorization", token(admin)).contentType(MediaType.APPLICATION_JSON)
                .content(input.deepCopy().put("idCard", invalid).toString())).andExpect(status().isBadRequest());
        }
        var created = mvc.perform(post("/api/v1/admin/users").header("Authorization", token(admin)).contentType(MediaType.APPLICATION_JSON)
            .content(input.put("idCard", "11010519491231002x").put("birthDate", "2000-01-01")
                .put("gender", "FEMALE").put("emergencyPhone", "13900004444").toString()))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.idCard").value("11010519491231002X"))
            .andExpect(jsonPath("$.gender").value("FEMALE"))
            .andExpect(jsonPath("$.birthDate").value("1949-12-31"))
            .andExpect(jsonPath("$.emergencyPhone").value("13900004444")).andReturn();
        String path = "/api/v1/admin/users/" + json.readTree(created.getResponse().getContentAsString()).path("id").asLong();
        for (String invalid : new String[]{"{\"idCard\":\"\"}", "{\"idCard\":null}", "{\"displayName\":\" \"}",
                "{\"phone\":\"138000044440\"}", "{\"phone\":\"\"}", "{\"clearManager\":true}"}) {
            mvc.perform(patch(path).header("Authorization", token(admin)).contentType(MediaType.APPLICATION_JSON)
                .content(invalid)).andExpect(status().isBadRequest());
        }
        mvc.perform(patch(path).header("Authorization", token(admin)).contentType(MediaType.APPLICATION_JSON)
            .content("{\"birthDate\":null}")).andExpect(status().isOk()).andExpect(jsonPath("$.birthDate").value("1949-12-31"));
    }

    @Test void changedEmergencyPhonesAreValidatedInBothEditorsWhileExistingValuesAreKept() throws Exception {
        var admin=login("admin"); var staff=login("staff");
        String path="/api/v1/admin/users/"+staff.path("userId").asLong();
        db.update("update users set id_card=?,emergency_phone=? where username='staff'", "000000198001010001", "123");
        mvc.perform(patch(path).header("Authorization",token(admin)).contentType(MediaType.APPLICATION_JSON)
            .content("{\"emergencyPhone\":\"123\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.emergencyPhone").value("123"))
            .andExpect(jsonPath("$.idCard").value("000000198001010001"));
        mvc.perform(patch(path).header("Authorization",token(admin)).contentType(MediaType.APPLICATION_JSON)
            .content("{\"emergencyPhone\":\"12012345678\"}")).andExpect(status().isBadRequest());
        mvc.perform(put("/api/v1/users/me").header("Authorization",token(staff)).contentType(MediaType.APPLICATION_JSON)
            .content("{\"emergencyPhone\":\"1391234567\"}")).andExpect(status().isBadRequest());
        mvc.perform(put("/api/v1/users/me").header("Authorization",token(staff)).contentType(MediaType.APPLICATION_JSON)
            .content("{\"emergencyPhone\":\"13912345678\"}")).andExpect(status().isOk())
            .andExpect(jsonPath("$.emergencyPhone").value("13912345678"));
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

    @Test void adminKeepsOwnNoticeNoteWhileBusinessEndpointStaysClosed() throws Exception {
        var admin = login("admin");
        var created = json.readTree(mvc.perform(post("/api/v1/admin/company-notices").header("Authorization", token(admin))
            .contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsString(Map.of("title", "管理员备注事项", "content", "正文", "enabled", true))))
            .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8));
        long id = created.get("id").asLong();
        // ADMIN 在管理端写本人备注
        mvc.perform(put("/api/v1/admin/company-notices/" + id + "/note").header("Authorization", token(admin))
            .contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsString(Map.of("content", "管理员的理解"))))
            .andExpect(status().isOk()).andExpect(jsonPath("$.myNote").value("管理员的理解"));
        mvc.perform(get("/api/v1/admin/company-notices/" + id + "/note").header("Authorization", token(admin)))
            .andExpect(status().isOk()).andExpect(jsonPath("$.myNote").value("管理员的理解"));
        // ADMIN 仍然进不了业务入口
        mvc.perform(put("/api/v1/company-notices/" + id + "/note").header("Authorization", token(admin))
            .contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsString(Map.of("content", "走业务端点"))))
            .andExpect(status().isForbidden());
        // 业务用户读不到 ADMIN 的备注，也不能用管理端备注端点
        var staff = login("staff");
        mvc.perform(get("/api/v1/company-notices/" + id).header("Authorization", token(staff)))
            .andExpect(status().isOk()).andExpect(jsonPath("$.myNote").value(org.hamcrest.Matchers.nullValue()));
        mvc.perform(put("/api/v1/admin/company-notices/" + id + "/note").header("Authorization", token(staff))
            .contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsString(Map.of("content", "越权备注"))))
            .andExpect(status().isForbidden());
        // 停用后 ADMIN 仍可读写本人备注
        mvc.perform(patch("/api/v1/admin/company-notices/" + id).header("Authorization", token(admin))
            .contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsString(Map.of("enabled", false))))
            .andExpect(status().isOk());
        mvc.perform(get("/api/v1/admin/company-notices/" + id + "/note").header("Authorization", token(admin)))
            .andExpect(status().isOk()).andExpect(jsonPath("$.myNote").value("管理员的理解"));
    }
}
