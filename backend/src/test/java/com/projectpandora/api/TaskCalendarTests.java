package com.projectpandora.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test") @Transactional
class TaskCalendarTests {
    private static final String PATH = "/api/v1/tasks/calendar";
    private static final ZoneId SHANGHAI = ZoneId.of("Asia/Shanghai");
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired JdbcTemplate db;

    @Test void scopesSummariesAndAgendaAndUsesShanghaiMidnightAndEffectiveLabels() throws Exception {
        var date = LocalDate.of(2036, 2, 29);
        var founder = token("founder");
        var staff = token("staff");
        var teamLead = token("team_lead");
        var manager = token("leader");
        long primary = userId("team_lead"), secondary = userId("staff");
        long label1 = label("Calendar A", "#123456"), label2 = label("Calendar B", "#123457");
        long shared = create(founder, "shared", start(date), "high", List.of(primary, secondary), List.of(label1, label2));
        create(founder, "previous day", start(date).minusSeconds(1), "normal", List.of(secondary), List.of());
        create(founder, "next day", start(date.plusDays(1)), "normal", List.of(secondary), List.of());
        create(founder, "not assigned to staff", start(date), "high", List.of(primary), List.of());
        long pending = create(teamLead, "pending", start(date).plusSeconds(60), "normal", List.of(secondary), List.of());
        long deleted = create(founder, "deleted", start(date).plusSeconds(60), "normal", List.of(secondary), List.of());
        mvc.perform(delete("/api/v1/tasks/" + deleted).header("Authorization", founder)).andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/tasks/" + pending).header("Authorization", manager)).andExpect(status().isOk());
        assertThat(read(summary(manager, date, date))).isEmpty();
        var result = read(summary(staff, date.minusDays(1), date.plusDays(1)));
        assertThat(result).extracting(day -> day.path("date").asText())
            .containsExactly(date.minusDays(1).toString(), date.toString(), date.plusDays(1).toString());
        var visible = result.get(1);
        assertThat(visible.path("total").asLong()).isEqualTo(1);
        var preview = visible.path("previews").get(0);
        assertThat(preview.path("id").asLong()).isEqualTo(shared);
        assertThat(preview.path("dueAt").asText()).isEqualTo(start(date).toString());
        assertThat(preview.path("labels")).extracting(label -> label.path("id").asLong()).containsExactly(label1, label2);
        assertThat(preview.has("detail")).isFalse();
        var agenda = read(agenda(staff, date, 0));
        assertThat(agenda.path("total").asLong()).isEqualTo(1);
        assertThat(agenda.path("items").get(0).path("task").path("id").asLong()).isEqualTo(shared);
        assertThat(agenda.path("items").get(0).path("labels")).isEqualTo(preview.path("labels"));
        assertThat(read(summary(teamLead, date, date)).get(0).path("total").asLong()).isEqualTo(2);
        summary(staff, date, date.plusDays(42)).andExpect(status().isBadRequest());
        summary(staff, date.plusDays(1), date).andExpect(status().isBadRequest());
        agenda(staff, date, -1).andExpect(status().isBadRequest());
    }

    @Test void keepsTaskOnEffectiveDayUntilRescheduleIsApproved() throws Exception {
        var date = LocalDate.of(2036, 3, 10);
        var staff = token("staff");
        var founder = token("founder");
        long id = create(founder, "Move task", start(date), "normal", List.of(userId("staff")), List.of());
        mvc.perform(put("/api/v1/tasks/" + id + "/due-date").header("Authorization",staff)
            .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsBytes(Map.of("dueAt",start(date.plusDays(1))))))
            .andExpect(status().isOk());
        var before = read(summary(staff, date, date.plusDays(1)));
        assertThat(before.size()).isEqualTo(1);
        assertThat(before.get(0).path("date").asText()).isEqualTo(date.toString());
        assertThat(before.get(0).path("previews").get(0).path("canReschedule").asBoolean()).isFalse();
        TaskPolicyTestSupport.decide(mvc, json, id, "team_lead", "approved");
        var after = read(summary(staff, date, date.plusDays(1)));
        assertThat(after.size()).isEqualTo(1);
        assertThat(after.get(0).path("date").asText()).isEqualTo(date.plusDays(1).toString());
        assertThat(after.get(0).path("previews").get(0).path("canReschedule").asBoolean()).isTrue();
    }

    @Test void busyDayHasBoundedPreviewAndStablePages() throws Exception {
        var date = LocalDate.of(2036, 5, 1);
        var staff = token("staff");
        seedTasks(date, userId("staff"), 10_000, false);
        var result = read(summary(staff, date.minusDays(1), date.plusDays(1))).get(0);
        assertThat(result.path("date").asText()).isEqualTo(date.toString());
        assertThat(result.path("total").asLong()).isEqualTo(10_000);
        var preview = result.path("previews");
        assertThat(preview.size()).isEqualTo(2);
        var first = read(agenda(staff, date, 0));
        var second = read(agenda(staff, date, 1));
        var last = read(agenda(staff, date, 333));
        assertThat(first.path("nextPage").asInt()).isEqualTo(1);
        assertThat(second.path("nextPage").asInt()).isEqualTo(2);
        assertThat(last.path("nextPage").isNull()).isTrue();
        assertThat(ids(first)).hasSize(30).startsWith(preview.get(0).path("id").asLong(), preview.get(1).path("id").asLong());
        assertThat(ids(second)).hasSize(30);
        assertThat(ids(last)).hasSize(10);
        var all = new HashSet<Long>();
        all.addAll(ids(first)); all.addAll(ids(second)); all.addAll(ids(last));
        assertThat(all).hasSize(70);
        assertThat(first.path("items").get(0).path("task").path("priority").asText()).isEqualTo("high");
        assertThat(last.path("items").get(9).path("task").path("status").asText()).isEqualTo("done");
    }

    @Test @org.junit.jupiter.api.condition.EnabledIfSystemProperty(named="pandora.calendar.benchmark", matches="true")
    void compareLegacyAndCalendarApi() throws Exception {
        var staff = token("staff");
        var date = LocalDate.of(2036, 6, 1);
        long owner = userId("staff");
        for (int size : List.of(100, 1_000, 10_000, -10_000)) {
            db.update("delete from dispatch_tasks where title like 'Calendar fixture %'");
            seedTasks(date, owner, Math.abs(size), size < 0);
            // One warm-up per API; report the median of three in-process requests.
            var legacy = measure(() -> mvc.perform(get("/api/v1/tasks").header("Authorization", staff)));
            var month = measure(() -> summary(staff, date, date.plusDays(34)));
            var day = measure(() -> agenda(staff, date, 0));
            System.out.printf("CALENDAR %5d %s legacy=%s summary=%s firstPage=%s%n",
                Math.abs(size), size < 0 ? "spread" : "one day", legacy, month, day);
        }
    }

    private interface Call { ResultActions run() throws Exception; }
    private String measure(Call call) throws Exception {
        var times = new ArrayList<Long>();
        int bytes = 0;
        for (int i=0; i<4; i++) {
            long begin=System.nanoTime();
            bytes=call.run().andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray().length;
            if(i>0)times.add((System.nanoTime()-begin)/1_000_000);
        }
        times.sort(Long::compareTo);
        return times.get(1)+"ms/"+bytes+"B";
    }
    private void seedTasks(LocalDate date, long owner, int size, boolean spread) {
        var rows = new ArrayList<Integer>();
        for (int i = 0; i < size; i++) rows.add(i);
        var jdbcZone = Calendar.getInstance(TimeZone.getTimeZone(SHANGHAI));
        db.batchUpdate("""
            INSERT INTO dispatch_tasks (title,detail,due_at,progress_note,created_by,assignee_id,priority,status)
            VALUES (?,'Calendar fixture detail',?,'',?,?,?,?)
            """, rows, 500, (statement,index) -> {
            statement.setString(1,"Calendar fixture "+index);
            statement.setTimestamp(2, Timestamp.from(start(spread ? date.plusDays(index % 35) : date).plusSeconds(index)), jdbcZone);
            statement.setLong(3,owner); statement.setLong(4,owner);
            statement.setString(5,index%3==0?"high":"normal");
            statement.setString(6,index%5==0?"done":"todo");
        });
    }
    private long create(String token, String title, Instant due, String priority, List<Long> assignees, List<Long> labels) throws Exception {
        var result=mvc.perform(post("/api/v1/tasks").header("Authorization",token).contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsBytes(Map.of("title",title,"detail","Calendar detail","priority",priority,
                "startAt",due.minusSeconds(3_600),"dueAt",due,"assigneeId",assignees.get(0),"assigneeIds",assignees,"labelIds",labels))))
            .andExpect(status().isCreated()).andReturn();
        return json.readTree(result.getResponse().getContentAsByteArray()).path("id").asLong();
    }
    private long label(String name, String color) {
        db.update("insert into work_labels(name,color,active) values(?,?,true)",name,color);
        return db.queryForObject("select id from work_labels where name=?", Long.class, name);
    }
    private ResultActions summary(String token, LocalDate from, LocalDate to) throws Exception {
        return mvc.perform(get(PATH).header("Authorization",token).param("from",from.toString()).param("to",to.toString()));
    }
    private ResultActions agenda(String token, LocalDate date, int page) throws Exception {
        return mvc.perform(get(PATH+"/day").header("Authorization",token).param("date",date.toString()).param("page",Integer.toString(page)));
    }
    private JsonNode read(ResultActions result) throws Exception {
        return json.readTree(result.andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray());
    }
    private static List<Long> ids(JsonNode result) {
        var ids = new ArrayList<Long>();
        result.path("items").forEach(item -> ids.add(item.path("task").path("id").asLong()));
        return ids;
    }
    private String token(String username) throws Exception { return TaskPolicyTestSupport.token(mvc,json,username); }
    private long userId(String name) { return db.queryForObject("select id from users where username=?",Long.class,name); }
    private static Instant start(LocalDate date) { return date.atStartOfDay(SHANGHAI).toInstant(); }
}
