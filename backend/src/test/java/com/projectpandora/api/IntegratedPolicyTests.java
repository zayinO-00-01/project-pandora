package com.projectpandora.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.projectpandora.api.log.WorkLogEntity;
import com.projectpandora.api.log.WorkLogRepository;
import com.projectpandora.api.user.*;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
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
class IntegratedPolicyTests {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired UserRepository users;
    @Autowired WorkLogRepository logs;
    @Autowired DemoOrgBootstrap bootstrap;
    LocalDate today() { return LocalDate.now(ZoneId.of("Asia/Shanghai")); }
    UserEntity user(String name) { return users.findByUsername(name).orElseThrow(); }
    String token(String name) throws Exception {
        return "Bearer " + json.readTree(mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsString(Map.of("username", name, "password", "demo1234"))))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString()).get("token").asText();
    }
    String logBody(LocalDate date, String state) throws Exception {
        return json.writeValueAsString(Map.of("logDate", date.toString(), "content", "工作记录", "status", state));
    }
    JsonNode createLog(String token, LocalDate date, String state) throws Exception {
        return json.readTree(mvc.perform(post("/api/v1/logs").header("Authorization", token)
            .contentType(MediaType.APPLICATION_JSON).content(logBody(date,state)))
            .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());
    }
    JsonNode request(String token, long target) throws Exception {
        return json.readTree(mvc.perform(post("/api/v1/view-requests").header("Authorization", token)
            .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("targetId", target, "reason", "学习工作安排"))))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
    }
    void decision(String token, long id, String state, int expected) throws Exception {
        mvc.perform(put("/api/v1/view-requests/"+id).header("Authorization", token).contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsString(Map.of("status",state)))).andExpect(status().is(expected));
    }
    void patchUser(String admin, long id, Map<String,Object> changes, int expected) throws Exception {
        mvc.perform(patch("/api/v1/admin/users/"+id).header("Authorization",admin).contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsString(changes))).andExpect(status().is(expected));
    }
    @Test void expiredDraftCannotBeSubmitted() throws Exception {
        var historic=new WorkLogEntity();historic.setUserId(user("staff").getId());historic.setLogDate(today().minusDays(4));
        historic.setContent("过期草稿");historic.setStatus("draft");
        historic.setCreatedAt(Instant.now().minus(25, java.time.temporal.ChronoUnit.HOURS));logs.saveAndFlush(historic);
        mvc.perform(post("/api/v1/logs/"+historic.getId()+"/submit").header("Authorization",token("staff")))
            .andExpect(status().isForbidden());
        assertThat(logs.findById(historic.getId()).orElseThrow().getStatus()).isEqualTo("draft");
    }
    @Test void organizationListsEveryCompanyMemberAsDispatcherTarget() throws Exception {
        var external=new UserEntity();external.setUsername("unlinked_assignee");external.setPasswordHash(user("staff").getPasswordHash());
        external.setRole(Role.STAFF);users.saveAndFlush(external);
        var peer=new UserEntity();peer.setUsername("peer_founder");peer.setPasswordHash(external.getPasswordHash());
        peer.setRole(Role.FOUNDER);users.saveAndFlush(peer);
        var disabled=new UserEntity();disabled.setUsername("disabled_assignee");disabled.setPasswordHash(external.getPasswordHash());
        disabled.setRole(Role.STAFF);disabled.setManagerId(user("team_lead").getId());disabled.setDisabled(true);users.saveAndFlush(disabled);
        var organization=json.readTree(mvc.perform(get("/api/v1/users/organization").header("Authorization",token("founder")))
            .andExpect(status().isOk()).andExpect(jsonPath("$.assignees").isArray()).andReturn().getResponse().getContentAsString());
        assertThat(organization.get("members").findValuesAsText("username"))
            .contains("unlinked_assignee","peer_founder","leader","team_lead","staff").doesNotContain("admin","disabled_assignee","founder");
        assertThat(organization.get("assignees").findValuesAsText("username"))
            .contains("leader","team_lead","staff","unlinked_assignee","peer_founder").doesNotContain("admin","disabled_assignee","founder");
        var leadOrg=json.readTree(mvc.perform(get("/api/v1/users/organization").header("Authorization",token("team_lead")))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        assertThat(leadOrg.get("assignees").findValuesAsText("username"))
            .contains("founder","leader","staff","unlinked_assignee","peer_founder").doesNotContain("team_lead","admin","disabled_assignee");
        mvc.perform(get("/api/v1/users/organization").header("Authorization",token("staff"))).andExpect(status().isOk())
            .andExpect(jsonPath("$.assignees.length()").value(0));
    }
    @Test void adminCannotUseAnyBusinessEntryPoint() throws Exception {
        String admin=token("admin");
        for(String path: new String[]{"/logs","/logs/integrity","/users","/users/organization","/tasks","/panels/map","/view-requests"})
            mvc.perform(get("/api/v1"+path).header("Authorization",admin)).andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/logs").header("Authorization",admin).contentType(MediaType.APPLICATION_JSON).content(logBody(today(),"submitted")))
            .andExpect(status().isForbidden());
    }
    @Test void writeWindowCoversThreeShanghaiDatesAndLocksHistoryAfter24h() throws Exception {
        String staff=token("staff");
        for(LocalDate date: new LocalDate[]{today().minusDays(2),today()}) createLog(staff,date,"submitted");
        for(LocalDate date: new LocalDate[]{today().minusDays(3),today().plusDays(1)})
            mvc.perform(post("/api/v1/logs").header("Authorization",staff).contentType(MediaType.APPLICATION_JSON).content(logBody(date,"draft")))
                .andExpect(status().isBadRequest());
        var historic=new WorkLogEntity(); historic.setUserId(user("staff").getId());historic.setLogDate(today().minusDays(4));
        historic.setContent("历史记录");historic.setStatus("draft");
        historic.setCreatedAt(Instant.now().minus(25, java.time.temporal.ChronoUnit.HOURS));logs.saveAndFlush(historic);
        mvc.perform(put("/api/v1/logs/"+historic.getId()).header("Authorization",staff).contentType(MediaType.APPLICATION_JSON).content(logBody(today(),"submitted")))
            .andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/logs/"+historic.getId()+"/submit").header("Authorization",staff)).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/logs").param("date",today().minusDays(4).toString()).header("Authorization",staff))
            .andExpect(status().isOk()).andExpect(jsonPath("$[0].content").value("历史记录"));
    }
    @Test void submittedEditKeepsIdAndStatusAndRejectsOutOfWindowDestination() throws Exception {
        String staff=token("staff");var log=createLog(staff,today(),"submitted");long id=log.get("id").asLong();
        mvc.perform(put("/api/v1/logs/"+id).header("Authorization",staff).contentType(MediaType.APPLICATION_JSON).content(logBody(today().minusDays(4),"submitted")))
            .andExpect(status().isBadRequest());
        mvc.perform(put("/api/v1/logs/"+id).header("Authorization",staff).contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsString(Map.of("logDate",today().toString(),"content","已修改"))))
            .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(id)).andExpect(jsonPath("$.status").value("submitted"));
    }
    @Test void founderSeesUnlinkedActiveBusinessAccountsWhileLeadersSeeActualDescendantsOnly() throws Exception {
        var external=new UserEntity(); external.setUsername("external_policy");external.setPasswordHash(user("staff").getPasswordHash());
        external.setRole(Role.STAFF);external.setDisplayName("其他成员");users.saveAndFlush(external);
        var disabled=new UserEntity();disabled.setUsername("disabled_policy");disabled.setPasswordHash(external.getPasswordHash());
        disabled.setRole(Role.STAFF);disabled.setManagerId(user("team_lead").getId());disabled.setDisabled(true);users.saveAndFlush(disabled);
        String founder=token("founder"), leader=token("leader");
        var visible=json.readTree(mvc.perform(get("/api/v1/users").header("Authorization",founder)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        assertThat(visible.findValuesAsText("username")).contains("external_policy","staff","team_lead","leader","founder").doesNotContain("admin","disabled_policy");
        mvc.perform(get("/api/v1/logs").param("userId",external.getId().toString()).header("Authorization",founder)).andExpect(status().isOk());
        mvc.perform(get("/api/v1/logs").param("userId",external.getId().toString()).header("Authorization",leader)).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/logs").param("userId",disabled.getId().toString()).header("Authorization",founder)).andExpect(status().isForbidden());
        var members=json.readTree(mvc.perform(get("/api/v1/users/organization").header("Authorization",leader)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        assertThat(members.get("members").findValuesAsText("username")).containsExactly("team_lead","staff");
        assertThat(members.get("manager").get("username").asText()).isEqualTo("founder");
    }
    @Test void approvedDirectManagerGrantReadsSubmittedLogsAndTasksOnlyAndRevokesImmediately() throws Exception {
        String staff=token("staff"), lead=token("team_lead"), founder=token("founder");long manager=user("team_lead").getId();
        var submitted=createLog(lead,today(),"submitted");createLog(lead,today(),"draft");
        var task=json.readTree(mvc.perform(post("/api/v1/tasks").header("Authorization",founder).contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsString(Map.of("title","上级任务","assigneeId",manager)))).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());
        var r=request(staff,manager);long id=r.get("id").asLong();
        mvc.perform(get("/api/v1/logs").param("userId",""+manager).header("Authorization",staff)).andExpect(status().isForbidden());
        decision(staff,id,"approved",403);decision(lead,id,"approved",200);decision(lead,id,"denied",409);
        mvc.perform(get("/api/v1/logs").param("userId",""+manager).header("Authorization",staff)).andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1)).andExpect(jsonPath("$[0].id").value(submitted.get("id").asLong()));
        mvc.perform(get("/api/v1/tasks").param("userId",""+manager).header("Authorization",staff)).andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value(task.get("id").asLong()));
        mvc.perform(put("/api/v1/logs/"+submitted.get("id")).header("Authorization",staff).contentType(MediaType.APPLICATION_JSON).content(logBody(today(),"submitted")))
            .andExpect(status().isForbidden());
        mvc.perform(put("/api/v1/tasks/"+task.get("id")+"/progress").header("Authorization",staff).contentType(MediaType.APPLICATION_JSON).content("{\"progress\":50,\"progressNote\":\"越权\"}"))
            .andExpect(status().isForbidden());
        decision(lead,id,"revoked",200);
        mvc.perform(get("/api/v1/logs").param("userId",""+manager).header("Authorization",staff)).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/tasks").param("userId",""+manager).header("Authorization",staff)).andExpect(status().isForbidden());
        request(staff,manager);
    }
    @Test void onlyActualDirectManagerCanBeRequestedAndDuplicatePendingIsConflict() throws Exception {
        String staff=token("staff");long manager=user("team_lead").getId();request(staff,manager);
        mvc.perform(post("/api/v1/view-requests").header("Authorization",staff).contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsString(Map.of("targetId",manager,"reason","重复")))).andExpect(status().isConflict());
        for(String name:new String[]{"staff","leader","founder","admin"})
            mvc.perform(post("/api/v1/view-requests").header("Authorization",staff).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("targetId",user(name).getId(),"reason","越级")))).andExpect(status().isForbidden());
    }
    @Test void managerChangeAndChangeBackDoNotReviveApprovedGrant() throws Exception {
        String admin=token("admin"),staff=token("staff"),lead=token("team_lead");long manager=user("team_lead").getId();
        var r=request(staff,manager);decision(lead,r.get("id").asLong(),"approved",200);
        patchUser(admin,user("staff").getId(),Map.of("managerId",user("leader").getId()),200);
        patchUser(admin,user("staff").getId(),Map.of("managerId",manager),200);
        mvc.perform(get("/api/v1/logs").param("userId",""+manager).header("Authorization",staff)).andExpect(status().isForbidden());
        request(staff,manager);
    }
    @Test void disableAndReenableInvalidatesGrantAndOldTokenCannotRunWhileDisabled() throws Exception {
        String admin=token("admin"),staff=token("staff"),lead=token("team_lead");long manager=user("team_lead").getId();
        var r=request(staff,manager);decision(lead,r.get("id").asLong(),"approved",200);
        patchUser(admin,user("staff").getId(),Map.of("disabled",true),200);
        mvc.perform(get("/api/v1/logs").header("Authorization",staff)).andExpect(status().isUnauthorized());
        patchUser(admin,user("staff").getId(),Map.of("disabled",false),200);
        mvc.perform(get("/api/v1/logs").param("userId",""+manager).header("Authorization",staff)).andExpect(status().isForbidden());
    }
    @Test void roleChangeInvalidatesPendingRequestEvenWhenDirectManagerRemains() throws Exception {
        String admin=token("admin");patchUser(admin,user("staff").getId(),Map.of("managerId",user("leader").getId()),200);
        var r=request(token("staff"),user("leader").getId());
        patchUser(admin,user("staff").getId(),Map.of("role","TEAM_LEAD"),200);
        decision(token("leader"),r.get("id").asLong(),"approved",409);
    }
    @Test void accountHierarchyRejectsCyclesWrongRanksAndInvalidatedChildEdges() throws Exception {
        String admin=token("admin");
        patchUser(admin,user("founder").getId(),Map.of("managerId",user("staff").getId()),400);
        patchUser(admin,user("staff").getId(),Map.of("managerId",user("admin").getId()),400);
        patchUser(admin,user("leader").getId(),Map.of("role","STAFF"),400);
        patchUser(admin,user("team_lead").getId(),Map.of("managerId",user("team_lead").getId()),400);
        patchUser(admin,user("admin").getId(),Map.of("managerId",user("founder").getId()),400);
    }
    @Test void trimmedDuplicateUsernameReturnsClientErrorWithoutInsertingAccount() throws Exception {
        mvc.perform(post("/api/v1/admin/users").header("Authorization",token("admin")).contentType(MediaType.APPLICATION_JSON)
            .content("{\"username\":\" staff \",\"password\":\"demo1234\",\"role\":\"STAFF\"}"))
            .andExpect(status().isBadRequest());
    }
    @Test void bootstrapDoesNotResetAdministratorsEditsOnRestart() throws Exception {
        var staff=user("staff");staff.setManagerId(null);staff.setDisabled(true);staff.setDisplayName("管理员修改");users.saveAndFlush(staff);
        var founder=user("founder");founder.setDisplayName("创始人自定义");users.saveAndFlush(founder);
        bootstrap.run(null);
        assertThat(user("staff").getManagerId()).isNull();assertThat(user("staff").isDisabled()).isTrue();
        assertThat(user("staff").getDisplayName()).isEqualTo("管理员修改");assertThat(user("founder").getDisplayName()).isEqualTo("创始人自定义");
    }
}
