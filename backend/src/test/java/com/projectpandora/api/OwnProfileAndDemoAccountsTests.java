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
        String request=json.writeValueAsString(Map.of("displayName","示例姓名","phone","010-12345678",
            "emergencyContact","示例联系人","emergencyPhone","12345678901","address","示例地址","role","FOUNDER"));
        mvc.perform(put("/api/v1/users/me").header("Authorization",staff).contentType(MediaType.APPLICATION_JSON).content(request))
            .andExpect(status().isOk()).andExpect(jsonPath("$.displayName").value("示例姓名"))
            .andExpect(jsonPath("$.role").value("STAFF")).andExpect(jsonPath("$.managerName").value("团队长"));
        mvc.perform(get("/api/v1/users/me").header("Authorization",staff))
            .andExpect(status().isOk()).andExpect(jsonPath("$.phone").value("010-12345678"));
        mvc.perform(get("/api/v1/users/me").header("Authorization",colleague))
            .andExpect(status().isOk()).andExpect(jsonPath("$.displayName").value("演示员工小陈"));
        mvc.perform(put("/api/v1/users/me").header("Authorization",staff).contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsString(Map.of("displayName","无效修改","phone","bad")))).andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/users/me").header("Authorization",staff))
            .andExpect(jsonPath("$.displayName").value("示例姓名"));
    }
}
