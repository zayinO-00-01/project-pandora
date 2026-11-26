package com.projectpandora.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.projectpandora.api.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test") @Transactional
class DirectoryDepartmentTests {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired UserRepository users;

    @Test void departmentMetadataFollowsTheReportingLineWithoutNarrowingTheDirectory() throws Exception {
        var staff = users.findByUsername("staff").orElseThrow();
        var lead = users.findByUsername("team_lead").orElseThrow();
        var department = users.findByUsername("leader").orElseThrow();
        staff.setManagerId(lead.getId()); lead.setManagerId(department.getId()); lead.setDisabled(true); department.setDisplayName("验收部门负责人");
        users.saveAndFlush(staff); users.saveAndFlush(lead); users.saveAndFlush(department);
        mvc.perform(get("/api/v1/directory").header("Authorization", token("staff")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[?(@.username == 'staff')].departmentId", contains(department.getId().intValue())))
            .andExpect(jsonPath("$[?(@.username == 'staff')].departmentName", contains("验收部门负责人的部门")))
            .andExpect(jsonPath("$[*].username", hasItem("founder")))
            .andExpect(jsonPath("$[*].username", not(hasItem("team_lead"))))
            .andExpect(jsonPath("$[*].emergencyPhone", empty()));
    }

    @Test void supervisorSeesTheRequestersNameInTheNotification() throws Exception {
        var staff = users.findByUsername("staff").orElseThrow();
        var lead = users.findByUsername("team_lead").orElseThrow();
        staff.setDisplayName("验收申请人"); staff.setManagerId(lead.getId()); users.saveAndFlush(staff);
        mvc.perform(post("/api/v1/view-requests").header("Authorization", token("staff"))
            .contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsString(java.util.Map.of("targetId", lead.getId(), "reason", "工作沟通"))))
            .andExpect(status().isOk());
        mvc.perform(get("/api/v1/notifications").header("Authorization", token("team_lead")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items[*].body", hasItem("验收申请人 申请查看你的工作信息")));
    }

    private String token(String name) throws Exception { return TaskPolicyTestSupport.token(mvc,json,name); }
}
