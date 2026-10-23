package com.projectpandora.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.projectpandora.api.log.*;
import com.projectpandora.api.security.JwtService;
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
    WorkLogEntity seed(LocalDate date,String status) { return seed(date,status,Instant.now()); }
    WorkLogEntity seed(LocalDate date,String status,Instant createdAt) {
        var e=new WorkLogEntity();e.setUserId(staff().getId());e.setLogDate(date);e.setContent("original history");e.setStatus(status);e.setCreatedAt(createdAt);return logs.saveAndFlush(e);
    }
    String body(LocalDate date) throws Exception {return json.writeValueAsString(Map.of("logDate",date.toString(),"content","updated","status","submitted"));}
    @Test void freshEntriesAreEditableAndSubmittableWithin24h() throws Exception {
        var e=seed(today(),"draft");
        mvc.perform(put("/api/v1/logs/"+e.getId()).header("Authorization",token(staff())).contentType(MediaType.APPLICATION_JSON).content(body(today())))
            .andExpect(status().isOk()).andExpect(jsonPath("$.content").value("updated"));
        mvc.perform(post("/api/v1/logs/"+e.getId()+"/submit").header("Authorization",token(staff()))).andExpect(status().isOk());
    }
    @Test void backfilledEntryWrittenTodayIsStillEditable() throws Exception {
        // 23rd writes the 21st entry: created now, so it stays editable for 24h even though dated 2 days ago.
        var e=seed(today().minusDays(2),"draft");
        mvc.perform(put("/api/v1/logs/"+e.getId()).header("Authorization",token(staff())).contentType(MediaType.APPLICATION_JSON).content(body(e.getLogDate())))
            .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(e.getId()));
    }
    @Test void historyOlderThan24hCanBeReadButNotEditedOrSubmitted() throws Exception {
        var old=Instant.now().minus(25, java.time.temporal.ChronoUnit.HOURS);
        var e=seed(today().minusDays(1),"draft",old);var path="/api/v1/logs/"+e.getId();
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
        for(var date:java.util.List.of(today().minusDays(3),today().plusDays(1))) {
            mvc.perform(put("/api/v1/logs/"+e.getId()).header("Authorization",token(staff())).contentType(MediaType.APPLICATION_JSON).content(body(date)))
                .andExpect(status().isBadRequest());
            mvc.perform(post("/api/v1/logs").header("Authorization",token(staff())).contentType(MediaType.APPLICATION_JSON).content(body(date)))
                .andExpect(status().isBadRequest());
        }
        assertThat(e.getContent()).isEqualTo("original history");assertThat(e.getLogDate()).isEqualTo(today());
    }
    @Test void creationAllowsTodayAndTwoPriorDaysOnly() throws Exception {
        for(int offset=0;offset<=2;offset++) {
            var date=today().minusDays(offset);
            mvc.perform(post("/api/v1/logs").header("Authorization",token(staff())).contentType(MediaType.APPLICATION_JSON).content(body(date)))
                .andExpect(status().isCreated());
        }
        mvc.perform(post("/api/v1/logs").header("Authorization",token(staff())).contentType(MediaType.APPLICATION_JSON).content(body(today().minusDays(3))))
            .andExpect(status().isBadRequest());
    }
    @Test void freshEntryCanMoveBetweenCreatableDaysButNotBeyond() throws Exception {
        var e=seed(today(),"draft");
        mvc.perform(put("/api/v1/logs/"+e.getId()).header("Authorization",token(staff())).contentType(MediaType.APPLICATION_JSON).content(body(today().minusDays(1))))
            .andExpect(status().isOk());
        mvc.perform(put("/api/v1/logs/"+e.getId()).header("Authorization",token(staff())).contentType(MediaType.APPLICATION_JSON).content(body(today().minusDays(3))))
            .andExpect(status().isBadRequest());
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
