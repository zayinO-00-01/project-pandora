package com.projectpandora.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.projectpandora.api.log.*;
import com.projectpandora.api.security.JwtService;
import com.projectpandora.api.user.*;
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
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test") @Transactional
class LogWriteWindowTests {
    @Autowired MockMvc mvc; @Autowired ObjectMapper json; @Autowired WorkLogRepository logs;
    @Autowired UserRepository users; @Autowired JwtService jwt;
    LocalDate today() {return LocalDate.now(ZoneId.of("Asia/Shanghai"));}
    UserEntity staff() {return users.findByUsername("staff").orElseThrow();}
    String token(UserEntity user) {return "Bearer "+jwt.createToken(user.getId(),user.getUsername(),user.getRole());}
    WorkLogEntity seed(LocalDate date,String status) {
        var e=new WorkLogEntity();e.setUserId(staff().getId());e.setLogDate(date);e.setContent("original history");e.setStatus(status);return logs.saveAndFlush(e);
    }
    String body(LocalDate date) throws Exception {return json.writeValueAsString(Map.of("logDate",date.toString(),"content","updated","status","submitted"));}
    @Test void onlyTodayAllowsEdits() throws Exception {
        for(var date:java.util.List.of(today())) {
            var e=seed(date,"draft");
            mvc.perform(put("/api/v1/logs/"+e.getId()).header("Authorization",token(staff())).contentType(MediaType.APPLICATION_JSON).content(body(date)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content").value("updated"));
            mvc.perform(post("/api/v1/logs/"+e.getId()+"/submit").header("Authorization",token(staff()))).andExpect(status().isOk());
        }
    }
    @Test void oldHistoryCanBeReadButCannotBeMovedOrSubmitted() throws Exception {
        var e=seed(today().minusDays(1),"draft");var path="/api/v1/logs/"+e.getId();
        mvc.perform(get("/api/v1/logs").param("date",e.getLogDate().toString()).header("Authorization",token(staff())))
            .andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(e.getId())).andExpect(jsonPath("$[0].content").value("original history"));
        mvc.perform(put(path).header("Authorization",token(staff())).contentType(MediaType.APPLICATION_JSON).content(body(today())))
            .andExpect(status().isForbidden());
        mvc.perform(post(path+"/submit").header("Authorization",token(staff()))).andExpect(status().isForbidden());
        assertThat(logs.findById(e.getId()).orElseThrow().getContent()).isEqualTo("original history");
        assertThat(e.getStatus()).isEqualTo("draft");
    }
    @Test void invalidTargetAndCreateDatesAreRejectedWithoutMutation() throws Exception {
        var e=seed(today(),"submitted");
        for(var date:java.util.List.of(today().minusDays(4),today().plusDays(1))) {
            mvc.perform(put("/api/v1/logs/"+e.getId()).header("Authorization",token(staff())).contentType(MediaType.APPLICATION_JSON).content(body(date)))
                .andExpect(status().isBadRequest());
            mvc.perform(post("/api/v1/logs").header("Authorization",token(staff())).contentType(MediaType.APPLICATION_JSON).content(body(date)))
                .andExpect(status().isBadRequest());
        }
        assertThat(e.getContent()).isEqualTo("original history");assertThat(e.getLogDate()).isEqualTo(today());
    }
    @Test void creationIncludesThreeDaysAgoButBackfillIsImmediatelyReadOnly() throws Exception {
        for(int offset=0;offset<=3;offset++) {
            var date=today().minusDays(offset);
            var response=mvc.perform(post("/api/v1/logs").header("Authorization",token(staff())).contentType(MediaType.APPLICATION_JSON).content(body(date)))
                .andExpect(status().isCreated()).andReturn();
            long id=json.readTree(response.getResponse().getContentAsString()).get("id").asLong();
            if(offset>0) {
                mvc.perform(put("/api/v1/logs/"+id).header("Authorization",token(staff())).contentType(MediaType.APPLICATION_JSON).content(body(today())))
                    .andExpect(status().isForbidden());
                mvc.perform(post("/api/v1/logs/"+id+"/submit").header("Authorization",token(staff()))).andExpect(status().isForbidden());
                assertThat(logs.findById(id).orElseThrow().getLogDate()).isEqualTo(date);
            }
        }
    }
    @Test void todayCannotBeMovedToAnOtherwiseCreatablePastDate() throws Exception {
        var e=seed(today(),"draft");
        mvc.perform(put("/api/v1/logs/"+e.getId()).header("Authorization",token(staff())).contentType(MediaType.APPLICATION_JSON).content(body(today().minusDays(1))))
            .andExpect(status().isBadRequest());
        assertThat(e.getLogDate()).isEqualTo(today());
    }
    @Test void serverDefaultTimezoneCannotChangeTheShanghaiBoundary() throws Exception {
        var original=java.util.TimeZone.getDefault();
        try {
            java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone("Etc/GMT+12"));
            mvc.perform(post("/api/v1/logs").header("Authorization",token(staff())).contentType(MediaType.APPLICATION_JSON).content(body(today())))
                .andExpect(status().isCreated());
        } finally {java.util.TimeZone.setDefault(original);}
    }
}
