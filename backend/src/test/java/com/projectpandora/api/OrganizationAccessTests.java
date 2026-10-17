package com.projectpandora.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.projectpandora.api.security.JwtService;
import com.projectpandora.api.user.*;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test") @Transactional
class OrganizationAccessTests {
    @Autowired MockMvc mvc; @Autowired UserRepository users; @Autowired JwtService jwt; @Autowired ObjectMapper json;
    UserEntity user(String name,Role role,UserEntity manager) {
        var u=new UserEntity();u.setUsername(name);u.setPasswordHash("not-a-login-password");u.setRole(role);u.setDisplayName(name);u.setManagerId(manager==null?null:manager.getId());return users.saveAndFlush(u);
    }
    String token(UserEntity u) {return "Bearer "+jwt.createToken(u.getId(),u.getUsername(),u.getRole());}
    String body(Object data) throws Exception {return json.writeValueAsString(data);}
    long apply(UserEntity requester,UserEntity target) throws Exception {
        var result=mvc.perform(post("/api/v1/view-requests").header("Authorization",token(requester)).contentType(MediaType.APPLICATION_JSON)
            .content(body(Map.of("targetId",target.getId(),"reason","核对工作依赖")))).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("pending")).andReturn();
        return json.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }
    void decision(UserEntity owner,long id,String status) throws Exception {
        mvc.perform(put("/api/v1/view-requests/"+id).header("Authorization",token(owner)).contentType(MediaType.APPLICATION_JSON).content(body(Map.of("status",status))))
            .andExpect(status().isOk()).andExpect(jsonPath("$.status").value(status));
    }
    void view(UserEntity viewer,UserEntity target,int code) throws Exception {
        for(String endpoint:new String[]{"logs","tasks"})mvc.perform(get("/api/v1/"+endpoint).param("userId",target.getId().toString()).header("Authorization",token(viewer))).andExpect(status().is(code));
    }
    @Test void downwardScopeUsesHierarchyAndCannotCrossDepartments() throws Exception {
        var f=user("scope-founder",Role.FOUNDER,null);var d=user("scope-dept",Role.DEPT_HEAD,f);var t=user("scope-team",Role.TEAM_LEAD,d);var s=user("scope-staff",Role.STAFF,t);
        var otherD=user("scope-otherdept",Role.DEPT_HEAD,f);var outsider=user("scope-outsider",Role.STAFF,otherD);
        view(f,s,200);view(d,s,200);view(t,s,200);view(d,outsider,403);view(t,d,403);view(s,t,403);view(s,outsider,403);
        mvc.perform(get("/api/v1/users/organization").header("Authorization",token(d))).andExpect(status().isOk()).andExpect(jsonPath("$.members.length()").value(2)).andExpect(jsonPath("$.manager.id").value(f.getId()));
    }
    @Test void onlyDirectManagerApprovesAndRevocationRemovesReadAccess() throws Exception {
        var f=user("approval-founder",Role.FOUNDER,null);var t=user("approval-team",Role.TEAM_LEAD,f);var s=user("approval-staff",Role.STAFF,t);
        mvc.perform(post("/api/v1/view-requests").header("Authorization",token(s)).contentType(MediaType.APPLICATION_JSON).content(body(Map.of("targetId",f.getId(),"reason","越级")))).andExpect(status().isForbidden());
        long id=apply(s,t);view(s,t,403);
        mvc.perform(put("/api/v1/view-requests/"+id).header("Authorization",token(f)).contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"approved\"}")).andExpect(status().isForbidden());
        decision(t,id,"approved");view(s,t,200);decision(t,id,"revoked");view(s,t,403);
        long repeated=apply(s,t);decision(t,repeated,"denied");view(s,t,403);
    }
    @Test void approvalNeverExposesDraftsOrAllowsEditingAndOrgChangeInvalidatesGrant() throws Exception {
        var t=user("privacy-team",Role.TEAM_LEAD,null);var s=user("privacy-staff",Role.STAFF,t);
        long id=apply(s,t);decision(t,id,"approved");
        long submitted=0;
        for(String state:new String[]{"draft","submitted"}) {
            var result=mvc.perform(post("/api/v1/logs").header("Authorization",token(t)).contentType(MediaType.APPLICATION_JSON).content(body(Map.of("logDate",LocalDate.now().minusDays(1).toString(),"content","private-content","status",state)))).andExpect(status().isCreated()).andReturn();
            if(state.equals("submitted"))submitted=json.readTree(result.getResponse().getContentAsString()).get("id").asLong();
        }
        mvc.perform(get("/api/v1/logs").param("userId",t.getId().toString()).header("Authorization",token(s))).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1)).andExpect(jsonPath("$[0].status").value("submitted"));
        mvc.perform(post("/api/v1/logs/"+submitted+"/submit").header("Authorization",token(s))).andExpect(status().isForbidden());
        s.setManagerId(null);users.saveAndFlush(s);view(s,t,403);
    }
    @Test void anonymousAndUnrelatedUsersCannotReadApplications() throws Exception {
        mvc.perform(get("/api/v1/view-requests")).andExpect(status().isUnauthorized());
        var t=user("history-team",Role.TEAM_LEAD,null);var s=user("history-staff",Role.STAFF,t);var other=user("history-other",Role.STAFF,null);apply(s,t);
        mvc.perform(get("/api/v1/view-requests").header("Authorization",token(other))).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
    }
}
