package com.projectpandora.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.projectpandora.api.security.JwtService;
import com.projectpandora.api.user.UserRepository;
import jakarta.persistence.EntityManager;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Independently rolled-back acceptance cases; IDs match midterm-cases.json and the report. */
@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test") @Transactional
class MidtermAcceptanceTests {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired JdbcTemplate db;
    @Autowired UserRepository users;
    @Autowired JwtService jwt;
    @Autowired EntityManager em;

    record AcceptanceCase(String id, String operation, int variant, String category,
                          String input, String expected) {
        @Override public String toString() { return id + " " + operation + " " + category; }
    }
    static Stream<AcceptanceCase> cases() throws Exception {
        try (var in = MidtermAcceptanceTests.class.getResourceAsStream("/midterm-cases.json")) {
            return Arrays.stream(new ObjectMapper().readValue(in, AcceptanceCase[].class));
        }
    }
    String token(String actor) {
        var user = users.findByUsername(actor).orElseThrow();
        return "Bearer " + jwt.createToken(user.getId(), user.getUsername(), user.getRole(), user.getTokenVersion());
    }
    long uid(String actor) { return users.findByUsername(actor).orElseThrow().getId(); }
    LocalDate today() { return LocalDate.now(ZoneId.of("Asia/Shanghai")); }
    JsonNode call(String verb, String path, String actor, Object body, int expected) throws Exception {
        MockHttpServletRequestBuilder request = switch (verb) {
            case "POST" -> post(path); case "PUT" -> put(path); case "PATCH" -> patch(path);
            case "DELETE" -> delete(path); default -> get(path);
        };
        if (actor != null) request.header("Authorization", token(actor));
        if (body != null) request.contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body));
        String response = mvc.perform(request).andExpect(status().is(expected)).andReturn()
                .getResponse().getContentAsString(StandardCharsets.UTF_8);
        return response.isBlank() ? json.nullNode() : json.readTree(response);
    }
    ObjectNode definition(String assignee) {
        return json.createObjectNode().put("title", "验收任务").put("detail", "验收说明")
                .put("priority", "normal").put("assigneeId", uid(assignee))
                .put("startAt", Instant.now().plusSeconds(3600).toString())
                .put("dueAt", Instant.now().plusSeconds(86400).toString());
    }
    JsonNode task(String creator, String assignee) throws Exception {
        return call("POST", "/api/v1/tasks", creator, definition(assignee), 201);
    }
    JsonNode effectiveStaffTask() throws Exception {
        var t = task("staff", "staff");
        return call("PUT", "/api/v1/tasks/" + t.path("id").asLong() + "/approval", "team_lead",
                Map.of("decision", "approved"), 200);
    }
    ObjectNode existingDefinition(JsonNode t) {
        return json.createObjectNode().put("title", t.path("title").asText()).put("detail", "修改说明")
                .put("assigneeId", t.path("assigneeId").asLong()).put("priority", "normal")
                .put("startAt", t.path("startAt").asText()).put("dueAt", t.path("dueAt").asText());
    }
    ObjectNode logBody(int offset) {
        return json.createObjectNode().put("logDate", today().plusDays(offset).toString())
                .put("content", "验收日志").put("status", "draft");
    }
    ObjectNode todoBody() {
        var b = json.createObjectNode().put("title", "验收事项").put("date", today().toString()).put("completed", false);
        b.putArray("labelIds"); return b;
    }
    long label() throws Exception {
        return call("POST", "/api/v1/labels", "founder", Map.of("name", "验收标签", "color", "#123abc"), 200).path("id").asLong();
    }
    void emptyNotices() {
        db.update("delete from company_notice_notes"); db.update("delete from company_notices");
    }
    JsonNode notice() throws Exception {
        return call("POST", "/api/v1/admin/company-notices", "admin", Map.of("title", "验收事项", "content", "验收内容"), 201);
    }
    long pendingProgress() throws Exception {
        var t = task("founder", "staff");
        call("PUT", "/api/v1/tasks/" + t.path("id").asLong() + "/progress", "staff",
                Map.of("progress", 50, "status", "doing", "progressNote", "进度申请"), 200);
        return TaskPolicyTestSupport.pendingReview(mvc, json, t.path("id").asLong(), "team_lead");
    }
    JsonNode pendingChange() throws Exception {
        var t = effectiveStaffTask();
        call("PUT", "/api/v1/tasks/" + t.path("id").asLong(), "staff", existingDefinition(t).put("title", "变更后"), 200);
        return t;
    }
    @ParameterizedTest(name = "{0}") @MethodSource("cases")
    void acceptance(AcceptanceCase c) throws Exception {
        System.out.println("MIDTERM_CASE=" + c.id());
        int v = c.variant();
        switch (c.operation()) {
            case "AuthController.login" -> {
                if (v == 1) { var u = users.findByUsername("staff").orElseThrow(); u.setDisabled(true); users.saveAndFlush(u); }
                var result = call("POST", "/api/v1/auth/login", null,
                        Map.of("username", "staff", "password", v == 2 ? "incorrect" : "demo1234"), v == 0 ? 200 : 401);
                if (v == 0) assertThat(result.path("token").asText()).isNotBlank();
            }
            case "TaskService.create" -> {
                var b = definition("staff");
                if (v == 1) b.put("title", "测".repeat(128)).put("startAt", b.path("dueAt").asText());
                if (v == 2) b.put("startAt", Instant.now().plusSeconds(172800).toString());
                var t = call("POST", "/api/v1/tasks", "founder", b, v == 2 ? 400 : 201);
                if (v != 2) { assertThat(t.path("title").asText()).isEqualTo(b.path("title").asText()); assertThat(t.path("approvalStatus").asText()).isEqualTo("approved"); }
            }
            case "TaskService.createBatch" -> {
                var items = json.createArrayNode();
                items.add(definition("staff"));
                if (v == 0) items.add(definition("team_lead"));
                if (v == 2) items.add(definition("staff"));
                var result = call("POST", "/api/v1/tasks/batch", "founder", Map.of("tasks", items), v == 2 ? 400 : 201);
                if (v != 2) assertThat(result.size()).isEqualTo(v == 0 ? 2 : 1);
            }
            case "TaskService.reschedule" -> {
                var t = task("founder", "founder"); String path = "/api/v1/tasks/" + t.path("id").asLong() + "/due-date";
                String due = v == 0 ? Instant.now().plusSeconds(172800).toString() : t.path("startAt").asText();
                if (v == 2) due = Instant.now().minusSeconds(60).toString();
                var result = call("PUT", path, "founder", Map.of("dueAt", due), v == 2 ? 400 : 200);
                if (v != 2) assertThat(Instant.parse(result.path("dueAt").asText())).isEqualTo(Instant.parse(due));
            }
            case "TaskService.review" -> {
                var t = task("staff", "staff"); String path = "/api/v1/tasks/" + t.path("id").asLong() + "/approval";
                if (v == 1) {
                    call("PUT", path, "team_lead", Map.of("decision", "approved"), 200);
                    call("PUT", path, "team_lead", Map.of("decision", "approved"), 409);
                } else {
                    var result = call("PUT", path, v == 2 ? "staff" : "team_lead", Map.of("decision", "approved"), v == 2 ? 403 : 200);
                    if (v == 0) assertThat(result.path("approvalStatus").asText()).isEqualTo("approved");
                }
            }
            case "TaskService.updateProgress" -> {
                var t = task("founder", "founder");
                var progress = Map.of("progress", v == 0 ? 50 : v == 1 ? 100 : 101, "status", v == 0 ? "doing" : "done", "progressNote", "验收进度");
                var result = call("PUT", "/api/v1/tasks/" + t.path("id").asLong() + "/progress", "founder", progress, v == 2 ? 400 : 200);
                if (v != 2) assertThat(result.path("progress").asInt()).isEqualTo(v == 0 ? 50 : 100);
            }
            case "TaskService.requestChange" -> {
                var t = task("founder", "staff"); var b = existingDefinition(t).put("title", "变更后");
                if (v == 1) { // Unchanged historical schedule must not prevent a text-only edit.
                    db.update("update dispatch_tasks set start_at=?,due_at=? where id=?",
                            java.sql.Timestamp.from(Instant.now().minusSeconds(3600)), java.sql.Timestamp.from(Instant.now().minusSeconds(60)), t.path("id").asLong());
                    em.clear(); t = call("GET", "/api/v1/tasks/" + t.path("id").asLong(), "founder", null, 200); b = existingDefinition(t).put("title", "逾期补充说明");
                }
                if (v == 2) b.put("assigneeId", uid("team_lead"));
                var result = call("PUT", "/api/v1/tasks/" + t.path("id").asLong(), "founder", b, v == 2 ? 400 : 200);
                if (v != 2) assertThat(result.path("title").asText()).isEqualTo(b.path("title").asText());
            }
            case "TaskService.withdraw" -> {
                var t = task("staff", "staff"); String path = "/api/v1/tasks/" + t.path("id").asLong() + "/withdraw";
                if (v == 1) { call("POST", path, "staff", null, 200); call("POST", path, "staff", null, 409); }
                else { var r = call("POST", path, v == 2 ? "founder" : "staff", null, v == 2 ? 403 : 200); if (v == 0) assertThat(r.path("approvalStatus").asText()).isEqualTo("rejected"); }
            }
            case "TaskService.approve", "TaskService.reject" -> {
                var t = pendingChange(); String action = c.operation().endsWith("approve") ? "approve" : "reject";
                String path = "/api/v1/tasks/" + t.path("id").asLong() + "/" + action;
                var body = Map.of("note", "验收审核理由");
                if (v == 1) { call("POST", path, "team_lead", body, 200); call("POST", path, "team_lead", body, 400); }
                else {
                    var result = call("POST", path, v == 2 ? "staff" : "team_lead", body, v == 2 ? 403 : 200);
                    if (v == 0) assertThat(result.path("title").asText()).isEqualTo(action.equals("approve") ? "变更后" : t.path("title").asText());
                }
            }
            case "TaskService.delete" -> {
                var t = task("founder", "staff"); String path = "/api/v1/tasks/" + t.path("id").asLong();
                if (v == 1) { call("DELETE", path, "founder", null, 204); call("DELETE", path, "founder", null, 404); }
                else { call("DELETE", path, v == 2 ? "staff" : "founder", null, v == 2 ? 403 : 204); call("GET", path, "founder", null, v == 2 ? 200 : 404); }
            }
            case "TaskService.addKeyword", "TaskService.changeKeyword" -> {
                var t = task("founder", "staff"); long id = t.path("id").asLong(); String path = "/api/v1/tasks/" + id + "/keywords";
                String text = v == 0 ? "测试词" : v == 1 ? "词".repeat(24) : "词".repeat(25);
                if (c.operation().endsWith("addKeyword")) call("POST", path, "founder", Map.of("text", text), v == 2 ? 400 : 200);
                else {
                    call("POST", path, "founder", Map.of("text", "原词"), 200);
                    var detail = call("GET", "/api/v1/tasks/" + id, "founder", null, 200);
                    long keywordId = detail.path("keywords").get(0).path("id").asLong();
                    call("PUT", path + "/" + keywordId, "founder", Map.of("text", text), v == 2 ? 400 : 200);
                }
                var detail = call("GET", "/api/v1/tasks/" + id, "founder", null, 200);
                if (v != 2) assertThat(detail.path("keywords").get(0).path("text").asText()).isEqualTo(text);
                else assertThat(detail.path("keywords").size()).isEqualTo(c.operation().endsWith("addKeyword") ? 0 : 1);
            }
            case "TaskService.decide" -> {
                long id = pendingProgress(); String path = "/api/v1/task-reviews/" + id;
                if (v == 1) { call("PUT", path, "team_lead", Map.of("status", "approved"), 200); call("PUT", path, "team_lead", Map.of("status", "approved"), 409); }
                else { var r = call("PUT", path, v == 2 ? "staff" : "team_lead", Map.of("status", "approved"), v == 2 ? 403 : 200); if (v == 0) assertThat(r.path("status").asText()).isEqualTo("approved"); }
            }
            case "TaskService.saveNote" -> {
                var t = task("founder", "staff"); String text = v == 0 ? "私密备注" : "注".repeat(v == 1 ? 2000 : 2001);
                String path = "/api/v1/tasks/" + t.path("id").asLong() + "/note";
                call("PUT", path, "staff", Map.of("content", text), v == 2 ? 400 : 200);
                assertThat(call("GET", path, "staff", null, 200).path("content").asText()).isEqualTo(v == 2 ? "" : text);
            }
            case "WorkLogController.create" -> {
                var b = logBody(v == 0 ? 0 : v == 1 ? -2 : -3);
                var r = call("POST", "/api/v1/logs", "staff", b, v == 2 ? 400 : 201);
                if (v != 2) assertThat(r.path("logDate").asText()).isEqualTo(b.path("logDate").asText());
            }
            case "WorkLogController.update", "WorkLogController.submit" -> {
                var b = logBody(0); var log = call("POST", "/api/v1/logs", "staff", b, 201); long id = log.path("id").asLong();
                if (v == 1) {
                    db.update("update work_logs set created_at=? where id=?", java.sql.Timestamp.from(Instant.now().minusSeconds(86401)), id); em.clear();
                }
                String path = "/api/v1/logs/" + id;
                if (c.operation().endsWith("update")) call("PUT", path, v == 2 ? "founder" : "staff", b.deepCopy().put("content", "修改后"), v == 0 ? 200 : 403);
                else call("POST", path + "/submit", v == 2 ? "founder" : "staff", null, v == 0 ? 200 : 403);
                em.clear();
                assertThat(db.queryForObject("select " + (c.operation().endsWith("update") ? "content" : "status") + " from work_logs where id=?", String.class, id))
                        .isEqualTo(c.operation().endsWith("update") ? (v == 0 ? "修改后" : "验收日志") : (v == 0 ? "submitted" : "draft"));
            }
            case "PersonalTodoController.create" -> {
                var b = todoBody().put("title", v == 0 ? "验收事项" : "事".repeat(v == 1 ? 200 : 201));
                var r = call("POST", "/api/v1/personal-todos", "staff", b, v == 2 ? 400 : 201);
                if (v != 2) assertThat(r.path("title").asText()).isEqualTo(b.path("title").asText());
            }
            case "PersonalTodoController.update" -> {
                var b = todoBody(); var t = call("POST", "/api/v1/personal-todos", "staff", b, 201); String path = "/api/v1/personal-todos/" + t.path("id").asText();
                var edited = b.deepCopy().put("title", "新标题"); edited.set("base", b.deepCopy());
                if (v != 0) {
                    call("PUT", path, "staff", edited, 200);
                    edited = b.deepCopy(); edited.set("base", b.deepCopy());
                    if (v == 1) edited.put("completed", true); else edited.put("title", "另一标题");
                }
                var r = call("PUT", path, "staff", edited, v == 2 ? 409 : 200);
                if (v != 2) { assertThat(r.path("title").asText()).isEqualTo("新标题"); assertThat(r.path("completed").asBoolean()).isEqualTo(v == 1); }
                else assertThat(call("GET", "/api/v1/personal-todos", "staff", null, 200).get(0).path("title").asText()).isEqualTo("新标题");
            }
            case "PersonalTodoController.delete" -> {
                var t = call("POST", "/api/v1/personal-todos", "staff", todoBody(), 201); String path = "/api/v1/personal-todos/" + t.path("id").asText();
                if (v == 1) { call("DELETE", path, "staff", null, 200); call("DELETE", path, "staff", null, 404); }
                else { call("DELETE", path, v == 2 ? "founder" : "staff", null, v == 2 ? 404 : 200); assertThat(call("GET", "/api/v1/personal-todos", "staff", null, 200).size()).isEqualTo(v == 2 ? 1 : 0); }
            }
            case "CompanyNoticeService.create" -> {
                emptyNotices(); if (v > 0) for (int i = 0; i < (v == 1 ? 9 : 10); i++) notice();
                noticeCreate(v == 2 ? 400 : 201);
                assertThat(db.queryForObject("select count(*) from company_notices where enabled=true", Integer.class)).isEqualTo(v == 0 ? 1 : 10);
            }
            case "CompanyNoticeService.update" -> {
                emptyNotices(); var n = notice(); String path = "/api/v1/admin/company-notices/" + n.path("id").asLong();
                String title = v == 0 ? "更新标题" : "事".repeat(v == 1 ? 128 : 129);
                call("PATCH", path, "admin", Map.of("title", title), v == 2 ? 400 : 200);
                assertThat(db.queryForObject("select title from company_notices where id=?", String.class, n.path("id").asLong())).isEqualTo(v == 2 ? "验收事项" : title);
            }
            case "CompanyNoticeService.saveNote" -> {
                emptyNotices(); var n = notice(); String path = "/api/v1/admin/company-notices/" + n.path("id").asLong() + "/note";
                String text = v == 0 ? "私密备注" : "注".repeat(v == 1 ? 2000 : 2001);
                call("PUT", path, "admin", Map.of("content", text), v == 2 ? 400 : 200);
                if (v != 2) assertThat(call("GET", path, "admin", null, 200).path("myNote").asText()).isEqualTo(text);
            }
            case "PersonalPriorityService.save" -> {
                var items = json.createArrayNode(); int count = v == 0 ? 1 : v == 1 ? 10 : 11;
                for (int i = 0; i < count; i++) items.add(json.createObjectNode().put("title", "事项" + i));
                var r = call("PUT", "/api/v1/priorities/personal", "staff", Map.of("revision", 0, "items", items), v == 2 ? 400 : 200);
                if (v != 2) { assertThat(r.path("items").size()).isEqualTo(count); assertThat(r.path("revision").asLong()).isEqualTo(1); }
            }
            case "LabelController.create", "LabelController.update" -> {
                String title = v == 0 ? "新标签" : "签".repeat(v == 1 ? 64 : 65);
                boolean update = c.operation().endsWith("update"); String path = "/api/v1/labels" + (update ? "/" + label() : "");
                var r = call(update ? "PUT" : "POST", path, "founder", Map.of("name", title, "color", "#123abc"), v == 2 ? 400 : 200);
                if (v != 2) { assertThat(r.path("name").asText()).isEqualTo(title); assertThat(r.path("color").asText()).isEqualTo("#123ABC"); }
            }
            case "LabelController.archive" -> {
                long id = label(); String path = "/api/v1/labels/" + id;
                if (v == 1) call("DELETE", path, "founder", null, 200);
                call("DELETE", path, v == 2 ? "staff" : "founder", null, v == 2 ? 403 : 200);
                assertThat(db.queryForObject("select active from work_labels where id=?", Boolean.class, id)).isEqualTo(v == 2);
            }
            case "ContentLabelsController.save" -> {
                var t = task("founder", "staff"); var ids = new ArrayList<Long>();
                for (int i = 0; i < (v == 0 ? 1 : v == 1 ? 12 : 13); i++) ids.add(label());
                var r = call("PUT", "/api/v1/content-labels/task/" + t.path("id").asLong(), "staff", Map.of("labelIds", ids), v == 2 ? 400 : 200);
                if (v != 2) {
                    long taskId = t.path("id").asLong();
                    assertThat(r.path("status").asText()).isEqualTo("pending");
                    assertThat(r.path("labels").size()).isZero();
                    var previousLabel = call("GET", "/api/v1/tasks/" + taskId, "staff", null, 200).path("label");
                    assertThat(previousLabel.isMissingNode() || previousLabel.isNull()).isTrue();
                    TaskPolicyTestSupport.decide(mvc, json, taskId, "team_lead", "approved");
                    var entries = call("GET", "/api/v1/content-labels", "staff", null, 200);
                    var entry = java.util.stream.StreamSupport.stream(entries.spliterator(), false)
                            .filter(e -> "task".equals(e.path("kind").asText()) && e.path("contentId").asLong() == taskId)
                            .findFirst().orElseThrow();
                    assertThat(entry.path("labels").size()).isEqualTo(ids.size());
                }
            }
            case "UserController.updateOwnProfile" -> {
                String name = call("GET", "/api/v1/users/me", "staff", null, 200).path("displayName").asText();
                var body = json.createObjectNode().put("address", "验收地址");
                if (v == 1) body.put("displayName", name);
                if (v == 2) body.put("displayName", name + "改");
                var r = call("PUT", "/api/v1/users/me", "staff", body, v == 2 ? 403 : 200);
                if (v != 2) {
                    assertThat(r.path("displayName").asText()).isEqualTo(name);
                    assertThat(r.path("address").asText()).isEqualTo("验收地址");
                }
            }
            case "UserController.changePassword", "ProfileController.password" -> {
                boolean legacy = c.operation().startsWith("User"); int min = legacy ? 6 : 8;
                String next = v == 0 ? "newpass123" : "a".repeat(v == 1 ? min : min - 1);
                call("PUT", legacy ? "/api/v1/users/me/password" : "/api/v1/profile/password", "staff", Map.of("currentPassword", "demo1234", "newPassword", next), v == 2 ? 400 : 200);
                if (v != 2) {
                    call("POST", "/api/v1/auth/login", null, Map.of("username", "staff", "password", next), 200);
                    call("POST", "/api/v1/auth/login", null, Map.of("username", "staff", "password", "demo1234"), 401);
                }
            }
            case "UserController.updateAvatar" -> {
                String value = v == 0 ? "FOX" : "头".repeat(v == 1 ? 8 : 9);
                var r = call("PUT", "/api/v1/users/me/avatar", "staff", Map.of("avatar", value), v == 2 ? 400 : 200);
                if (v != 2) assertThat(r.path("avatar").asText()).isEqualTo(value);
            }
            case "ProfileController.avatar" -> {
                String value = v == 1 ? "" : v == 2 ? "data:image/png;base64,AAAA" : png();
                var r = call("PUT", "/api/v1/profile/avatar", "staff", Map.of("avatarData", value), v == 2 ? 400 : 200);
                if (v == 0) assertThat(r.path("avatarData").asText()).isEqualTo(value);
                if (v == 1) assertThat(r.path("avatarData").isNull()).isTrue();
            }
            case "AdminUserController.create" -> {
                var b = json.createObjectNode().put("username", v == 0 ? "acceptance" : v == 1 ? "abc" : "ab")
                        .put("password", "demo1234").put("role", "STAFF").put("phone", "13900009999").put("managerId", uid("team_lead"))
                        .put("displayName", "验收成员").put("idCard", TestIdentity.card(440 + v));
                var r = call("POST", "/api/v1/admin/users", "admin", b, v == 2 ? 400 : 201);
                if (v != 2) assertThat(r.path("username").asText()).isEqualTo(b.path("username").asText());
            }
            case "AdminUserController.update" -> {
                long id = uid("staff");
                if (v == 1) call("PATCH", "/api/v1/admin/users/" + id, "admin", Map.of("birthDate", "2000-01-01", "hireDate", "2026-01-01"), 200);
                ObjectNode b = json.createObjectNode().put("displayName", "更新成员");
                if (v == 1) { b.putNull("birthDate"); b.putNull("hireDate"); }
                if (v == 2) b.put("managerId", id);
                var r = call("PATCH", "/api/v1/admin/users/" + id, "admin", b, v == 2 ? 400 : 200);
                if (v == 0) assertThat(r.path("displayName").asText()).isEqualTo("更新成员");
                if (v == 1) { assertThat(r.path("birthDate").isNull()).isTrue(); assertThat(r.path("hireDate").isNull()).isTrue(); }
            }
            case "ViewRequestController.create" -> {
                String reason = v == 0 ? "工作协作" : "因".repeat(v == 1 ? 500 : 501);
                var r = call("POST", "/api/v1/view-requests", "staff", Map.of("targetId", uid("team_lead"), "reason", reason), v == 2 ? 400 : 200);
                if (v != 2) assertThat(r.path("status").asText()).isEqualTo("pending");
            }
            case "ViewRequestController.decide" -> {
                var r = call("POST", "/api/v1/view-requests", "staff", Map.of("targetId", uid("team_lead"), "reason", "协作"), 200);
                String path = "/api/v1/view-requests/" + r.path("id").asLong();
                if (v == 1) { call("PUT", path, "team_lead", Map.of("status", "approved"), 200); call("PUT", path, "team_lead", Map.of("status", "approved"), 409); }
                else { var result = call("PUT", path, v == 2 ? "staff" : "team_lead", Map.of("status", "approved"), v == 2 ? 403 : 200); if (v == 0) assertThat(result.path("status").asText()).isEqualTo("approved"); }
            }
            case "NotificationService.markRead" -> {
                long id = notification("staff"); String path = "/api/v1/notifications/" + id + "/read";
                if (v == 1) call("POST", path, "staff", null, 204);
                call("POST", path, v == 2 ? "founder" : "staff", null, v == 2 ? 403 : 204);
                em.flush();
                assertThat(db.queryForObject("select read_at from notifications where id=?", Object.class, id) != null).isEqualTo(v != 2);
            }
            case "NotificationService.markAllRead" -> {
                db.update("delete from notifications where user_id=?", uid("staff")); long other = notification("founder");
                if (v != 1) { notification("staff"); notification("staff"); }
                call("POST", "/api/v1/notifications/read-all", v == 2 ? null : "staff", null, v == 2 ? 401 : 204);
                em.flush();
                assertThat(db.queryForObject("select count(*) from notifications where user_id=? and read_at is null", Integer.class, uid("staff"))).isEqualTo(v == 2 ? 2 : 0);
                assertThat(db.queryForObject("select read_at from notifications where id=?", Object.class, other)).isNull();
            }
            default -> throw new AssertionError("Unimplemented acceptance case " + c.operation());
        }
    }
    void noticeCreate(int code) throws Exception {
        call("POST", "/api/v1/admin/company-notices", "admin", Map.of("title", "最后一条", "content", "验收内容"), code);
    }
    String png() throws Exception {
        var bytes = new java.io.ByteArrayOutputStream();
        javax.imageio.ImageIO.write(new java.awt.image.BufferedImage(1, 1, java.awt.image.BufferedImage.TYPE_INT_RGB), "png", bytes);
        return "data:image/png;base64," + Base64.getEncoder().encodeToString(bytes.toByteArray());
    }
    long notification(String actor) {
        db.update("insert into notifications(user_id,type,title,body,created_at) values(?,?,?,?,?)", uid(actor), "test", "验收通知", "验收内容", java.sql.Timestamp.from(Instant.now()));
        return db.queryForObject("select max(id) from notifications where user_id=?", Long.class, uid(actor));
    }
}
