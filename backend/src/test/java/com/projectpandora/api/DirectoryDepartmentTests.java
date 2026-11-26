package com.projectpandora.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.projectpandora.api.user.*;
import org.springframework.security.crypto.password.PasswordEncoder;
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
    @Autowired PasswordEncoder passwords;

    @Test void departmentMetadataFollowsTheReportingLineWithoutNarrowingTheDirectory() throws Exception {
        var staff = users.findByUsername("staff").orElseThrow();
        var lead = users.findByUsername("team_lead").orElseThrow();
        var department = users.findByUsername("leader").orElseThrow();
        staff.setManagerId(lead.getId()); lead.setManagerId(department.getId()); lead.setDisabled(true); department.setDisplayName("验收部门负责人"); department.setDepartmentName("验收部门");
        users.saveAndFlush(staff); users.saveAndFlush(lead); users.saveAndFlush(department);
        mvc.perform(get("/api/v1/directory").header("Authorization", token("founder")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[?(@.username == 'staff')].departmentId", contains(department.getId().intValue())))
            .andExpect(jsonPath("$[?(@.username == 'staff')].departmentName", contains("验收部门")))
            .andExpect(jsonPath("$[*].username", not(hasItem("founder"))))
            .andExpect(jsonPath("$[*].username", not(hasItem("team_lead"))))
            .andExpect(jsonPath("$[*].emergencyPhone", empty()));
    }

    @Test void directoryUsesTheFourRoleScopesWithoutChangingWorkOrTaskAssignment() throws Exception {
        var first = users.findByUsername("leader").orElseThrow();
        var localLead = users.findByUsername("team_lead").orElseThrow();
        var staff = users.findByUsername("staff").orElseThrow();
        var founder = users.findByUsername("founder").orElseThrow();
        var localPeer = person("local_peer",Role.STAFF,localLead);
        var secondLead = person("local_other_team",Role.TEAM_LEAD,first);
        var sameDeptPeer = person("other_team_peer",Role.STAFF,secondLead);
        var otherDept = person("other_department",Role.DEPT_HEAD,founder);
        var otherLead = person("other_department_team",Role.TEAM_LEAD,otherDept);
        var otherStaff = person("other_department_staff",Role.STAFF,otherLead);
        var unassigned = person("unassigned_employee",Role.STAFF,null);
        String file = "/api/v1/directory";
        mvc.perform(get(file).header("Authorization",token("staff"))).andExpect(status().isOk())
            .andExpect(jsonPath("$[*].username", containsInAnyOrder("team_lead","local_peer","other_team_peer")))
            .andExpect(jsonPath("$[?(@.username == 'local_peer')].managerName", contains("团队长")))
            .andExpect(jsonPath("$[?(@.username == 'other_team_peer')].managerName", not(hasItem("local_other_team"))));
        String leadToken = token("team_lead");
        mvc.perform(get(file).header("Authorization",leadToken)).andExpect(status().isOk())
            .andExpect(jsonPath("$[*].username", containsInAnyOrder("leader","local_other_team","other_department_team","staff","local_peer","other_team_peer")))
            .andExpect(jsonPath("$[?(@.username == 'local_peer')].canViewWork", contains(true)))
            .andExpect(jsonPath("$[?(@.username == 'other_team_peer')].canViewWork", contains(false)));
        for (String viewer : new String[]{"leader","founder"}) {
            mvc.perform(get(file).header("Authorization",token(viewer))).andExpect(status().isOk())
                .andExpect(jsonPath("$[*].username", hasItems("staff","other_department_staff","unassigned_employee")));
        }
        mvc.perform(get(file).header("Authorization",token("unassigned_employee"))).andExpect(status().isOk())
            .andExpect(jsonPath("$", empty()));
        mvc.perform(get("/api/v1/logs").param("userId",sameDeptPeer.getId().toString()).header("Authorization",leadToken))
            .andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/users/organization").header("Authorization",leadToken)).andExpect(status().isOk())
            .andExpect(jsonPath("$.assignees[*].username", hasItem(otherStaff.getUsername())));
    }

    private UserEntity person(String username,Role role,UserEntity manager) {
        var user=new UserEntity(); user.setUsername(username); user.setDisplayName(username);
        user.setRole(role); user.setPasswordHash(passwords.encode("demo1234"));
        user.setManagerId(manager==null?null:manager.getId()); return users.saveAndFlush(user);
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
