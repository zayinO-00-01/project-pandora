package com.projectpandora.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import java.util.Map;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// Bootstrap runs before test transactions; keep the opt-in accounts out of other test contexts.
@SpringBootTest(properties={
    "pandora.demo.extra-accounts=true",
    "spring.datasource.url=jdbc:h2:mem:pandora_demo_accounts_ci;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class OwnProfileAndDemoAccountsTests {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;

    @Test void demoStaffCanEditOnlyTheirOwnPersonalFields() throws Exception {
        String staff=TaskPolicyTestSupport.token(mvc,json,"staff_demo2");
        String colleague=TaskPolicyTestSupport.token(mvc,json,"staff_demo3");
        long staffId=json.readTree(mvc.perform(get("/api/v1/users/me").header("Authorization",staff))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString()).get("id").asLong();
        String admin=TaskPolicyTestSupport.token(mvc,json,"admin");
        mvc.perform(patch("/api/v1/admin/users/"+staffId).header("Authorization",admin).contentType(MediaType.APPLICATION_JSON)
            .content("{\"phone\":\"13800007654\",\"idCard\":\"11010519491231002X\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.idCard").value("11010519491231002X"));
        String request=json.writeValueAsString(Map.of("displayName","示例姓名",
            "emergencyContact","示例联系人","emergencyPhone","010-12345678","address","示例地址","role","FOUNDER"));
        mvc.perform(put("/api/v1/users/me").header("Authorization",staff).contentType(MediaType.APPLICATION_JSON).content(request))
            .andExpect(status().isOk()).andExpect(jsonPath("$.displayName").value("示例姓名"))
            .andExpect(jsonPath("$.emergencyPhone").value("010-12345678"))
            .andExpect(jsonPath("$.role").value("STAFF")).andExpect(jsonPath("$.managerName").value("团队长"));
        mvc.perform(get("/api/v1/users/me").header("Authorization",staff))
            .andExpect(status().isOk()).andExpect(jsonPath("$.phone").value("13800007654"));
        mvc.perform(get("/api/v1/users/me").header("Authorization",colleague))
            .andExpect(status().isOk()).andExpect(jsonPath("$.displayName").value("演示员工小陈"));
        long colleagueId=json.readTree(mvc.perform(get("/api/v1/users/me").header("Authorization",colleague))
            .andReturn().getResponse().getContentAsString()).get("id").asLong();
        mvc.perform(patch("/api/v1/admin/users/"+colleagueId).header("Authorization",admin).contentType(MediaType.APPLICATION_JSON)
            .content("{\"phone\":\"13900007655\"}")).andExpect(status().isOk());
        mvc.perform(put("/api/v1/users/me").header("Authorization",staff).contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsString(Map.of("displayName","示例姓名","phone"," 13900007654 ","managerId",colleagueId))))
            .andExpect(status().isOk()).andExpect(jsonPath("$.phone").value("13900007654"))
            .andExpect(jsonPath("$.managerName").value("团队长")).andExpect(jsonPath("$.idCard").doesNotHaveJsonPath());
        for (String phone : java.util.List.of("13900007655","010-12345678","")) {
            mvc.perform(put("/api/v1/users/me").header("Authorization",staff).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("displayName","无效修改","phone",phone)))).andExpect(status().isBadRequest());
        }
        mvc.perform(get("/api/v1/users/me").header("Authorization",staff))
            .andExpect(status().isOk()).andExpect(jsonPath("$.displayName").value("示例姓名"))
            .andExpect(jsonPath("$.phone").value("13900007654")).andExpect(jsonPath("$.idCard").doesNotHaveJsonPath());
    }
}
