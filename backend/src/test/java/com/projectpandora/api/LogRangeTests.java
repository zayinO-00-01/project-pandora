package com.projectpandora.api;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.projectpandora.api.log.*;
import com.projectpandora.api.security.JwtService;
import com.projectpandora.api.user.*;
import java.time.*;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test") @Transactional
class LogRangeTests {
    @Autowired MockMvc mvc; @Autowired ObjectMapper json; @Autowired WorkLogRepository logs;
    @Autowired UserRepository users; @Autowired JwtService jwt;
    LocalDate today(){return LocalDate.now(ZoneId.of("Asia/Shanghai"));}
    String token(String name){var u=users.findByUsername(name).orElseThrow();return "Bearer "+jwt.createToken(u.getId(),u.getUsername(),u.getRole());}
    String body(LocalDate start,LocalDate end,String status) throws Exception{return json.writeValueAsString(Map.of("logDate",start.toString(),"endDate",end.toString(),"content","连续三天病假，未到岗","status",status));}
    @Test void oneRangeCoversAllDatesAndKeepsDatesFixedWithoutDuplicateRows() throws Exception {
        var result=mvc.perform(post("/api/v1/logs").header("Authorization",token("staff")).contentType(MediaType.APPLICATION_JSON).content(body(today().minusDays(2),today(),"submitted")))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.endDate").value(today().toString())).andReturn();
        long id=json.readTree(result.getResponse().getContentAsString()).get("id").asLong();
        var row=logs.findById(id).orElseThrow();
        for(int i=0;i<3;i++) {
            var date=today().minusDays(i);
            assertThat(logs.coveringDate(row.getUserId(),date)).extracting(WorkLogEntity::getId).contains(id);
            mvc.perform(get("/api/v1/logs").param("date",date.toString()).header("Authorization",token("staff")))
                .andExpect(status().isOk()).andExpect(jsonPath("$[?(@.id == "+id+")].endDate").value(org.hamcrest.Matchers.hasItem(today().toString())));
        }
        mvc.perform(put("/api/v1/logs/"+id).header("Authorization",token("staff")).contentType(MediaType.APPLICATION_JSON).content(body(today(),today(),"submitted"))).andExpect(status().isBadRequest());
        mvc.perform(post("/api/v1/logs/"+id+"/submit").header("Authorization",token("staff"))).andExpect(status().isOk());
        assertThat(logs.findById(id).orElseThrow().getLogDate()).isEqualTo(today().minusDays(2));
    }
    @Test void rejectsReverseExpiredFutureAndPreservesHistoricalDraftPrivacy() throws Exception {
        for(var pair:java.util.List.of(new LocalDate[]{today(),today().minusDays(1)},new LocalDate[]{today().minusDays(4),today()},new LocalDate[]{today(),today().plusDays(1)}))
            mvc.perform(post("/api/v1/logs").header("Authorization",token("staff")).contentType(MediaType.APPLICATION_JSON).content(body(pair[0],pair[1],"submitted"))).andExpect(status().isBadRequest());
        mvc.perform(post("/api/v1/logs").header("Authorization",token("staff")).contentType(MediaType.APPLICATION_JSON).content(body(today().minusDays(2),today(),"draft"))).andExpect(status().isCreated()).andExpect(jsonPath("$.status").value("draft"));
        mvc.perform(post("/api/v1/logs").header("Authorization",token("staff")).contentType(MediaType.APPLICATION_JSON).content(body(today().minusDays(1),today().minusDays(1),"draft"))).andExpect(status().isCreated()).andExpect(jsonPath("$.status").value("draft"));
        mvc.perform(post("/api/v1/logs").header("Authorization",token("staff")).contentType(MediaType.APPLICATION_JSON).content(body(today(),today(),"draft"))).andExpect(status().isCreated()).andExpect(jsonPath("$.status").value("draft"));
    }
    @Test void newRecordsRemainEditableAndOtherAuthorsCannotWrite() throws Exception {
        var result=mvc.perform(post("/api/v1/logs").header("Authorization",token("staff")).contentType(MediaType.APPLICATION_JSON).content(body(today(),today(),"draft"))).andExpect(status().isCreated()).andReturn();
        long id=json.readTree(result.getResponse().getContentAsString()).get("id").asLong();
        mvc.perform(put("/api/v1/logs/"+id).header("Authorization",token("staff")).contentType(MediaType.APPLICATION_JSON).content(body(today(),today(),"submitted"))).andExpect(status().isOk());
        mvc.perform(put("/api/v1/logs/"+id).header("Authorization",token("leader")).contentType(MediaType.APPLICATION_JSON).content(body(today(),today(),"submitted"))).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/logs/integrity").header("Authorization",token("staff"))).andExpect(jsonPath("$.supportsDateRanges").value(true));
    }

    @Test void migrationBackfillsOldSingleDayWithoutChangingContent() throws Exception {
        String url="jdbc:h2:mem:range_migration_"+System.nanoTime()+";MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1";
        org.flywaydb.core.Flyway.configure().dataSource(url,"sa","").target("6").load().migrate();
        try(var c=java.sql.DriverManager.getConnection(url,"sa","");var statement=c.createStatement()) {
            statement.executeUpdate("INSERT INTO work_logs(user_id,log_date,content,status) SELECT id,DATE '2026-01-01','original','submitted' FROM users WHERE username='staff'");
        }
        org.flywaydb.core.Flyway.configure().dataSource(url,"sa","").load().migrate();
        try(var c=java.sql.DriverManager.getConnection(url,"sa","");var statement=c.createStatement();var rows=statement.executeQuery("SELECT log_date,end_date,content FROM work_logs")) {
            assertThat(rows.next()).isTrue();assertThat(rows.getDate(1)).isEqualTo(rows.getDate(2));assertThat(rows.getString(3)).isEqualTo("original");
        }
    }
}
