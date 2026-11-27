package com.projectpandora.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.projectpandora.api.security.JwtService;
import com.projectpandora.api.user.*;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.*;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:pandora_crm;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
    "pandora.crm.contract-reminders-cron=-"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CrmAcceptanceTests {
    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");
    private static final String HEADER = "source,memberNumber,orderNumber,product,status,amount,refundAmount,paidOn,refundedOn,name,phone,ownerId\n";
    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired JwtService jwt;
    @Autowired ObjectMapper json;

    private UserEntity user(String name, Role role, UserEntity manager) {
        var u = new UserEntity();
        u.setUsername("phase2-" + name);
        u.setDisplayName(name);
        u.setPasswordHash("test");
        u.setRole(role);
        u.setManagerId(manager == null ? null : manager.getId());
        return users.saveAndFlush(u);
    }
    private String token(UserEntity user) {
        return "Bearer " + jwt.createToken(user.getId(), user.getUsername(), user.getRole(), user.getTokenVersion());
    }
    private JsonNode call(UserEntity user, String method, String path, Object body, int code) throws Exception {
        var req = request(HttpMethod.valueOf(method), "/api/v1" + path).header("Authorization", token(user));
        if (body != null) req.contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body));
        var response = mvc.perform(req).andExpect(status().is(code)).andReturn().getResponse();
        return json.readTree(response.getContentAsString(StandardCharsets.UTF_8));
    }
    private JsonNode crm(UserEntity user, String method, String path, Object body, int code) throws Exception {
        return call(user, method, "/crm" + path, body, code);
    }
    private void grants(UserEntity admin, UserEntity target, String... rights) throws Exception {
        crm(admin, "PUT", "/admin/permissions/" + target.getId(), Map.of("permissions", rights), 200);
    }
    private Map<String, Object> company(String name, String phone) {
        return Map.of("kind", "COMPANY", "name", name, "contactName", "李女士", "phone", phone);
    }
    private Map<String, Object> service(long customer, String kind, Object... people) {
        return Map.of("customerId", customer, "kind", kind, "scheduledAt", LocalDateTime.now(ZONE).withNano(0).toString(),
            "description", "确认活动安排", "participantIds", people);
    }
    private long complete(UserEntity owner, long customer, String kind) throws Exception {
        long service = crm(owner, "POST", "/services", service(customer, kind), 201).path("service").path("id").asLong();
        crm(owner, "PUT", "/services/" + service + "/status", Map.of("status", "COMPLETED"), 200);
        return service;
    }
    private JsonNode upload(UserEntity operator, String row, boolean apply) throws Exception {
        return crm(operator, "POST", "/admin/imports", Map.of("csv", HEADER + row + "\n", "apply", apply), 200);
    }

    @Test void serviceScopeDuplicateReviewHandoverAndCompletedRecord() throws Exception {
        var founder = user("workspace-founder", Role.FOUNDER, null);
        var admin = user("workspace-admin", Role.ADMIN, null);
        var head = user("workspace-head", Role.DEPT_HEAD, founder);
        var otherHead = user("workspace-other-head", Role.DEPT_HEAD, founder);
        var owner = user("workspace-owner", Role.STAFF, head);
        var other = user("workspace-other", Role.STAFF, otherHead);
        var outsider = user("workspace-outsider", Role.STAFF, otherHead);
        var participant = user("workspace-participant", Role.STAFF, head);
        var review = user("workspace-review", Role.STAFF, head);
        grants(admin, review, "CUSTOMER_REVIEW");
        long customer = crm(owner, "POST", "/customers", company("青禾演示企业", "19900000021"), 201).path("id").asLong();
        var input = service(customer, "VISIT", participant.getId());
        long first = crm(owner, "POST", "/services", input, 201).path("service").path("id").asLong();
        assertThat(crm(head, "GET", "/customers/" + customer, null, 200).path("customer").path("phone").asText()).isEqualTo("****0021");
        assertThat(crm(participant, "GET", "/customers/" + customer, null, 200).path("customer").path("phone").asText()).startsWith("****");
        crm(outsider, "GET", "/customers/" + customer, null, 404);
        crm(admin, "GET", "/customers/" + customer, null, 403);
        crm(head, "PUT", "/services/" + first, input, 403);
        crm(participant, "POST", "/services/" + first + "/notes", Map.of("text", "不能代改"), 403);
        var duplicate = crm(other, "POST", "/customers", company("青禾演示企业", "19900000021"), 409);
        assertThat(duplicate.toString()).doesNotContain("19900000021", "青禾演示企业", "李女士");
        var match = Map.of("kind", "COMPANY", "name", "青禾演示企业");
        crm(other, "POST", "/customer-matches", match, 403);
        assertThat(crm(review, "POST", "/customer-matches", match, 200).get(0).path("id").asLong()).isEqualTo(customer);
        crm(review, "PUT", "/customer-matches/" + customer + "/assign", Map.of("userId", other.getId(), "reason", "确认是同一企业"), 204);
        crm(other, "POST", "/services", service(customer, "SALON"), 201);
        assertThat(crm(head, "GET", "/customers/" + customer, null, 200).path("services").size()).isEqualTo(1);
        assertThat(crm(founder, "GET", "/customers/" + customer, null, 200).path("services").size()).isEqualTo(2);

        crm(owner, "POST", "/services/" + first + "/transfer", Map.of("ownerId", other.getId(), "reason", "改由活动组接待"), 200);
        crm(owner, "GET", "/services/" + first, null, 404);
        var transferred = crm(other, "GET", "/services/" + first, null, 200);
        assertThat(transferred.path("history").get(0).path("detail").asText()).contains("改由活动组接待");
        assertThat(transferred.path("service").path("participants").get(0).path("id").asLong()).isEqualTo(participant.getId());
        crm(other, "PUT", "/services/" + first + "/status", Map.of("status", "COMPLETED"), 200);
        crm(other, "PUT", "/services/" + first, input, 409);
        var finished = crm(other, "POST", "/services/" + first + "/notes", Map.of("text", "参访后安排沟通"), 200);
        assertThat(finished.path("history").get(0).path("detail").asText()).isEqualTo("参访后安排沟通");
        var correction = Map.of("actualAt", finished.path("service").path("actualAt").asText(),
            "description", "补正参访说明", "reason", "核对接待记录");
        crm(other, "POST", "/services/" + first + "/corrections", correction, 403);
        grants(admin, other, "SERVICE_CORRECT");
        crm(other, "POST", "/services/" + first + "/corrections", correction, 200);
        var corrected = crm(other, "GET", "/services/" + first, null, 200);
        assertThat(corrected.path("service").path("description").asText()).isEqualTo("补正参访说明");
        assertThat(corrected.path("history").get(0).path("detail").asText()).contains("确认活动安排", "核对接待记录");

        long task = call(founder, "POST", "/tasks", Map.of(
            "title", "参访跟进", "detail", "整理会后清单", "assigneeId", founder.getId(), "priority", "normal",
            "startAt", Instant.now().toString(), "dueAt", Instant.now().plusSeconds(86400).toString()), 201).path("id").asLong();
        crm(other, "POST", "/services/" + first + "/tasks", Map.of("taskId", task), 404);
        long own = crm(founder, "POST", "/services", service(customer, "OTHER"), 201).path("service").path("id").asLong();
        var linked = crm(founder, "POST", "/services/" + own + "/tasks", Map.of("taskId", task), 200);
        assertThat(linked.path("tasks").get(0).path("id").asLong()).isEqualTo(task);
        assertThat(call(founder, "GET", "/tasks/" + task, null, 200).path("status").asText()).isEqualTo("todo");
    }

    @Test void importPreviewReplayRefundAndReversibleCardUse() throws Exception {
        var admin = user("card-admin", Role.ADMIN, null);
        var operator = user("card-operator", Role.STAFF, null);
        grants(admin, operator, "SHOP_IMPORT", "CARD_CORRECT");
        var today = LocalDate.now(ZONE);
        String middle = "testshop,member1,order1,ANNUAL_CARD,", tail = ",\"商城,演示客户\",19900000022," + operator.getId() + "\n";
        String paid = middle + "PAID,600,," + today + "," + tail;
        var preview = upload(operator, paid, false);
        assertThat(preview.path("ready").asBoolean()).isTrue();
        assertThat(preview.path("committed").asBoolean()).isFalse();
        assertThat(crm(operator, "GET", "/customers", null, 200).size()).isZero();
        var badBatch = upload(operator, paid + "not,enough,columns", true);
        assertThat(badBatch.path("committed").asBoolean()).isFalse();
        assertThat(crm(operator, "GET", "/customers", null, 200).size()).isZero();
        assertThat(upload(operator, paid, true).path("committed").asBoolean()).isTrue();
        assertThat(upload(operator, paid, true).path("rows").get(0).path("result").asText()).isEqualTo("SKIPPED");
        long customer = crm(operator, "GET", "/customers", null, 200).get(0).path("id").asLong();
        var order = crm(operator, "GET", "/customers/" + customer + "/orders", null, 200).get(0);
        long orderId = order.path("id").asLong();
        assertThat(order.path("remaining").asInt()).isEqualTo(6);
        long completed = complete(operator, customer, "SHOW");
        var used = crm(operator, "POST", "/orders/" + orderId + "/uses", Map.of("serviceId", completed), 200);
        assertThat(used.path("remaining").asInt()).isEqualTo(5);
        long useId = used.path("uses").get(0).path("id").asLong();
        crm(operator, "POST", "/orders/" + orderId + "/uses", Map.of("serviceId", completed), 409);
        var undo = crm(operator, "POST", "/orders/" + orderId + "/uses/" + useId + "/reverse", Map.of("reason", "误登记"), 200);
        assertThat(undo.path("remaining").asInt()).isEqualTo(6);
        assertThat(undo.path("uses").get(0).path("reverseReason").asText()).isEqualTo("误登记");
        crm(operator, "POST", "/orders/" + orderId + "/uses/" + useId + "/reverse", Map.of("reason", "重复撤销"), 409);
        var partial = upload(operator, middle + "PARTIAL_REFUND,600,100," + today + "," + today + tail, true);
        assertThat(partial.path("committed").asBoolean()).isTrue();
        assertThat(crm(operator, "GET", "/customers/" + customer + "/orders", null, 200).get(0).path("usable").asBoolean()).isTrue();
        var full = upload(operator, middle + "REFUNDED,600,600," + today + "," + today + tail, true);
        assertThat(full.path("committed").asBoolean()).isTrue();
        var refunded = crm(operator, "GET", "/customers/" + customer + "/orders", null, 200).get(0);
        assertThat(refunded.path("usable").asBoolean()).isFalse();
        assertThat(refunded.path("uses").size()).isEqualTo(1);
        crm(operator, "GET", "/admin/shop-cash", null, 403);
        crm(operator, "POST", "/orders/" + orderId + "/uses", Map.of("serviceId", complete(operator, customer, "SHOW")), 409);
    }

    @Test void handoverNotifiesNewOwnerButAnOldNotificationDoesNotGrantAccess() throws Exception {
        var owner = user("handover-owner", Role.STAFF, null);
        var recipient = user("handover-recipient", Role.STAFF, null);
        var next = user("handover-next", Role.STAFF, null);
        long customer = crm(owner, "POST", "/customers", company("交接演示企业", "19900000024"), 201).path("id").asLong();
        long sid = crm(owner, "POST", "/services", service(customer, "VISIT"), 201).path("service").path("id").asLong();
        String path = "/services/" + sid;
        crm(owner, "POST", path + "/transfer", Map.of("ownerId", recipient.getId(), "reason", "跨部门交接"), 200);
        var inbox = call(recipient, "GET", "/notifications", null, 200);
        assertThat(inbox.path("items").size()).isEqualTo(1);
        var notice = inbox.path("items").get(0);
        assertThat(notice.path("type").asText()).isEqualTo("crm-service-transfer");
        assertThat(notice.path("taskId").isNull()).isTrue();
        assertThat(notice.path("referenceId").asLong()).isEqualTo(sid);
        assertThat(notice.toString()).doesNotContain("交接演示企业", "19900000024", "跨部门交接");
        assertThat(call(next, "GET", "/notifications", null, 200).path("items").isEmpty()).isTrue();
        assertThat(crm(recipient, "GET", path, null, 200).path("service").path("owner").path("id").asLong())
            .isEqualTo(recipient.getId());

        crm(recipient, "POST", path + "/transfer", Map.of("ownerId", next.getId(), "reason", "再次交接"), 200);
        var oldNotice = call(recipient, "GET", "/notifications", null, 200).path("items").get(0);
        crm(recipient, "GET", "/services/" + oldNotice.path("referenceId").asLong(), null, 404);
        call(recipient, "POST", "/notifications/" + oldNotice.path("id").asLong() + "/read", null, 204);
        assertThat(call(recipient, "GET", "/notifications", null, 200).path("unreadCount").asLong()).isZero();
        crm(next, "GET", path, null, 200);
    }

    private Map<String, Object> cashInput(String direction, int amount, LocalDate date, String note) {
        return Map.of("direction", direction, "amountCents", amount, "bookedOn", date, "note", note, "requestId", UUID.randomUUID());
    }

    @Test void receiptAndRefundDoNotDependOnServiceCompletion() throws Exception {
        var admin = user("cash-admin", Role.ADMIN, null);
        var founder = user("cash-founder", Role.FOUNDER, null);
        var operator = user("cash-operator", Role.STAFF, founder);
        var finance = user("cash-finance", Role.STAFF, founder);
        grants(admin, finance, "FINANCE");
        long customer = crm(operator, "POST", "/customers", company("财务演示企业", "19900000023"), 201).path("id").asLong();
        long sid = complete(operator, customer, "VISIT");
        String ledger = "/admin/receivables/" + sid;
        crm(finance, "PUT", ledger, Map.of("amountCents", 300_000), 204);
        var service = crm(operator, "GET", "/services/" + sid, null, 200);
        assertThat(service.path("service").path("paymentStatus").asText()).isEqualTo("UNPAID");
        assertThat(service.path("service").has("receivableCents")).isFalse();
        crm(operator, "GET", ledger + "/cash", null, 403);
        crm(founder, "GET", ledger + "/cash", null, 403);
        crm(admin, "GET", "/admin/revenue", null, 403);
        var today = LocalDate.now(ZONE);
        var receipt = cashInput("RECEIPT", 300_000, today, "参访已到账");
        crm(finance, "POST", ledger + "/cash", receipt, 204);
        crm(finance, "POST", ledger + "/cash", receipt, 204);
        var refund = cashInput("REFUND", 50_000, today, "退回部分款项");
        crm(finance, "POST", ledger + "/cash", refund, 204);
        crm(finance, "POST", ledger + "/cash", refund, 204);
        crm(finance, "POST", ledger + "/cash", cashInput("REFUND", 260_000, today, "超过实收"), 409);
        assertThat(crm(finance, "GET", ledger + "/cash", null, 200).size()).isEqualTo(2);
        var byKind = crm(founder, "GET", "/admin/revenue", null, 200).path("byKind");
        var visit = new ArrayList<JsonNode>();
        byKind.forEach(row -> { if (row.path("kind").asText().equals("VISIT")) visit.add(row); });
        assertThat(visit).hasSize(1);
        assertThat(visit.get(0).path("receipts").asLong() - visit.get(0).path("refunds").asLong()).isEqualTo(250_000);
    }

    @Test void simultaneousCashRetriesWriteOnceAndIdenticalNewBookingsRemainIndependent() throws Exception {
        var admin = user("cash-retry-admin", Role.ADMIN, null);
        var finance = user("cash-retry-finance", Role.STAFF, null);
        grants(admin, finance, "FINANCE");
        long customer = crm(finance, "POST", "/customers", company("流水防重企业", "19900000025"), 201).path("id").asLong();
        long sid = complete(finance, customer, "SALON");
        long other = complete(finance, customer, "SALON");
        String ledger = "/admin/receivables/" + sid;
        String otherLedger = "/admin/receivables/" + other;
        crm(finance, "PUT", ledger, Map.of("amountCents", 100_000), 204);
        crm(finance, "PUT", otherLedger, Map.of("amountCents", 100_000), 204);
        var date = LocalDate.now(ZONE);
        var receipt = cashInput("RECEIPT", 10_000, date, "同额分笔到账");
        var threads = Executors.newFixedThreadPool(2);
        try {
            Callable<JsonNode> attempt = () -> crm(finance, "POST", ledger + "/cash", receipt, 204);
            for (var result : threads.invokeAll(List.of(attempt, attempt), 15, TimeUnit.SECONDS)) result.get();
        } finally { threads.shutdownNow(); }
        assertThat(crm(finance, "GET", ledger + "/cash", null, 200).size()).isEqualTo(1);
        var changed = new HashMap<>(receipt);
        changed.put("amountCents", 20_000);
        crm(finance, "POST", ledger + "/cash", changed, 409);
        crm(finance, "POST", otherLedger + "/cash", receipt, 409);
        changed.remove("requestId");
        crm(finance, "POST", ledger + "/cash", changed, 400);
        crm(finance, "POST", ledger + "/cash", cashInput("RECEIPT", 10_000, date, "同额分笔到账"), 204);
        assertThat(crm(finance, "GET", ledger + "/cash", null, 200).size()).isEqualTo(2);
        assertThat(crm(finance, "GET", otherLedger + "/cash", null, 200).isEmpty()).isTrue();
        grants(admin, finance);
        crm(finance, "POST", ledger + "/cash", receipt, 403);
    }

    @Test void contractReadIsIndependentOfRoleAndReminderDoesNotRepeat() throws Exception {
        var admin = user("contract-admin", Role.ADMIN, null);
        var hr = user("contract-hr", Role.STAFF, null);
        var staff = user("contract-staff", Role.STAFF, null);
        grants(admin, hr, "CONTRACTS");
        crm(hr, "PUT", "/admin/staff/" + staff.getId(), Map.of("department", "展演部", "jobTitle", "课程演示"), 204);
        var date = LocalDate.now(ZONE);
        byte[] data = "%PDF-1.4\nsample\n%%EOF".getBytes(StandardCharsets.US_ASCII);
        var upload = mvc.perform(multipart("/api/v1/crm/admin/contracts")
            .file(new MockMultipartFile("file", "demo.pdf", "application/pdf", data))
            .param("userId", staff.getId().toString()).param("title", "课程演示合同")
            .param("startOn", date.minusMonths(1).toString()).param("endOn", date.plusDays(30).toString())
            .header("Authorization", token(hr))).andExpect(status().isCreated()).andReturn();
        long id = json.readTree(upload.getResponse().getContentAsString()).path("id").asLong();
        mvc.perform(get("/api/v1/crm/admin/contracts/" + id + "/pdf").header("Authorization", token(hr)))
            .andExpect(status().isOk()).andExpect(header().string("Cache-Control", "no-store")).andExpect(content().bytes(data));
        crm(admin, "GET", "/admin/contracts/" + id + "/pdf", null, 403);
        crm(staff, "GET", "/admin/contracts", null, 403);
        assertThat(crm(hr, "POST", "/admin/contract-reminders", null, 200).path("sent").asInt()).isEqualTo(1);
        assertThat(crm(hr, "POST", "/admin/contract-reminders", null, 200).path("sent").asInt()).isZero();
        var notifications = call(hr, "GET", "/notifications", null, 200).path("items");
        assertThat(notifications.size()).isEqualTo(1);
        assertThat(notifications.get(0).path("type").asText()).isEqualTo("contract-reminder");
        assertThat(notifications.toString()).doesNotContain("课程演示合同", staff.getDisplayName());
        grants(admin, hr);
        crm(hr, "GET", "/admin/contracts/" + id + "/pdf", null, 403);
    }
}
