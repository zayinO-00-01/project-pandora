package com.projectpandora.api.crm;

import static com.projectpandora.api.crm.CrmStore.*;
import com.projectpandora.api.crm.CrmAccess.Member;
import com.projectpandora.api.crm.CrmAccess.Scope;
import com.projectpandora.api.notification.NotificationService;
import com.projectpandora.api.task.TaskService;
import java.time.LocalDateTime;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class CrmWorkspace {
    static final Set<String> SERVICE_KINDS = Set.of("VISIT", "BANQUET", "SALON", "SHOW", "OTHER");
    private final CrmStore db;
    private final CrmAccess access;
    private final TaskService tasks;
    private final NotificationService notifications;

    CrmWorkspace(CrmStore db, CrmAccess access, TaskService tasks, NotificationService notifications) {
        this.db = db;
        this.access = access;
        this.tasks = tasks;
        this.notifications = notifications;
    }

    record CustomerInput(String kind, String name, String contactName, String phone) {}
    record Customer(long id, String kind, String name, String contactName, String phone, long createdBy,
                    LocalDateTime createdAt) {}
    record CustomerView(long id, String kind, String name, String contactName, String phone,
                        boolean editable, LocalDateTime createdAt) {}
    record ServiceInput(Long customerId, String kind, LocalDateTime scheduledAt, String description, Set<Long> participantIds) {}
    record ServiceRow(long id, long customerId, long ownerId, String kind, String status, LocalDateTime scheduledAt,
                      LocalDateTime actualAt, String description, long receivableCents, LocalDateTime createdAt) {}
    record Participant(long serviceId, long userId) {}
    record OrderOwner(long customerId, long ownerId) {}
    record ServiceView(long id, long customerId, String customerName, Member owner, String kind, String status,
                       LocalDateTime scheduledAt, LocalDateTime actualAt, String description, List<Member> participants,
                       String paymentStatus, boolean editable, boolean canTransfer, boolean canCorrect) {}
    record Event(long id, long actorId, String eventType, String detail, LocalDateTime recordedAt) {}
    record EventView(long id, String actor, String eventType, String detail, LocalDateTime recordedAt) {}
    record TaskLink(long id, String title) {}
    record ServiceDetail(ServiceView service, List<EventView> history, List<TaskLink> tasks) {}
    record CustomerDetail(CustomerView customer, List<ServiceView> services) {}
    record ChangeStatus(String status, LocalDateTime actualAt) {}
    record Transfer(Long ownerId, String reason) {}
    record Correction(Long customerId, Long ownerId, LocalDateTime actualAt, String description, String reason) {}
    record Match(String kind, String name, String phone) {}
    record Payment(long serviceId, long net) {}

    private record Visibility(List<ServiceRow> services, Map<Long, Set<Long>> participants,
                              Map<Long, List<ServiceRow>> servicesByCustomer, Map<Long, Set<Long>> orderOwners,
                              Set<Long> linkedCustomers, Map<Long, Long> balances) {}

    private Visibility visibility(Scope scope) {
        var services = db.rows(ServiceRow.class, "SELECT * FROM crm_services ORDER BY scheduled_at DESC,id DESC");
        Map<Long, Set<Long>> participants = new HashMap<>(), orderOwners = new HashMap<>();
        for (var p : db.rows(Participant.class, "SELECT service_id,user_id FROM crm_service_members"))
            participants.computeIfAbsent(p.serviceId(), k -> new HashSet<>()).add(p.userId());
        Map<Long, List<ServiceRow>> grouped = new HashMap<>();
        services.forEach(s -> grouped.computeIfAbsent(s.customerId(), k -> new ArrayList<>()).add(s));
        for (var order : db.rows(OrderOwner.class, "SELECT DISTINCT customer_id,owner_id FROM crm_orders"))
            orderOwners.computeIfAbsent(order.customerId(), k -> new HashSet<>()).add(order.ownerId());
        Map<Long, Long> balances = new HashMap<>();
        for (var p : db.rows(Payment.class, """
            SELECT service_id,SUM(CASE WHEN direction='RECEIPT' THEN amount_cents ELSE -amount_cents END) AS net
            FROM crm_cash WHERE service_id IS NOT NULL GROUP BY service_id
            """)) balances.put(p.serviceId(), p.net());
        return new Visibility(services, participants, grouped, orderOwners,
            Set.copyOf(db.values(Long.class, "SELECT customer_id FROM crm_customer_links WHERE user_id=?", scope.id())), balances);
    }

    private boolean visible(Scope scope, Visibility all, ServiceRow service) {
        return scope.maySeeOwner(service.ownerId()) || all.participants().getOrDefault(service.id(), Set.of()).contains(scope.id());
    }

    private boolean manages(Scope scope, Visibility all, Customer customer) {
        var services = all.servicesByCustomer().getOrDefault(customer.id(), List.of());
        var orders = all.orderOwners().getOrDefault(customer.id(), Set.of());
        return (services.isEmpty() && orders.isEmpty() && customer.createdBy() == scope.id())
            || services.stream().anyMatch(s -> s.ownerId() == scope.id()) || orders.contains(scope.id());
    }

    private boolean visible(Scope scope, Visibility all, Customer customer) {
        return scope.founder() || manages(scope, all, customer) || all.linkedCustomers().contains(customer.id())
            || all.servicesByCustomer().getOrDefault(customer.id(), List.of()).stream().anyMatch(s -> visible(scope, all, s))
            || all.orderOwners().getOrDefault(customer.id(), Set.of()).stream().anyMatch(scope::maySeeOwner);
    }

    private CustomerView view(Scope scope, Visibility all, Customer row) {
        boolean full = manages(scope, all, row);
        String phone = row.phone(), contact = row.contactName();
        if (!full) {
            phone = phone.isBlank() ? "" : "****" + (phone.length() > 4 ? phone.substring(phone.length() - 4) : "");
            contact = contact.isBlank() ? "" : contact.substring(0, contact.offsetByCodePoints(0, 1)) + "**";
        }
        return new CustomerView(row.id(), row.kind(), row.name(), contact, phone, full, row.createdAt());
    }

    @Transactional(readOnly = true)
    public List<CustomerView> customers(String search) {
        var scope = access.business();
        var all = visibility(scope);
        String q = key(optional(search, 120, "搜索内容"));
        return db.rows(Customer.class, "SELECT * FROM crm_customers ORDER BY name_key,id").stream()
            .filter(c -> visible(scope, all, c))
            .map(c -> view(scope, all, c))
            .filter(c -> q.isEmpty() || key(c.name()).contains(q) || key(c.contactName()).contains(q) || key(c.phone()).contains(q))
            .toList();
    }

    @Transactional(readOnly = true)
    public CustomerDetail detail(long id) {
        var scope = access.business();
        var all = visibility(scope);
        var customer = visibleCustomer(scope, all, id);
        return new CustomerDetail(view(scope, all, customer), all.servicesByCustomer().getOrDefault(id, List.of())
            .stream().filter(s -> visible(scope, all, s)).map(s -> view(scope, all, s, customer)).toList());
    }

    Customer customer(long id) {
        return db.one(Customer.class, "SELECT * FROM crm_customers WHERE id=?", id).orElseThrow(CrmStore::missing);
    }
    private Customer visibleCustomer(Scope scope, Visibility all, long id) {
        var customer = customer(id);
        if (!visible(scope, all, customer)) throw missing();
        return customer;
    }

    static CustomerInput clean(CustomerInput input) {
        if (!"PERSON".equals(input.kind()) && !"COMPANY".equals(input.kind())) throw bad("请选择客户类型");
        String name = required(input.name(), 120, "客户名称");
        String contact = "COMPANY".equals(input.kind()) ? required(input.contactName(), 120, "企业对接人") : "";
        String phone = optional(input.phone(), 30, "联系电话");
        if (!phone.isEmpty() && (!phone.matches("[0-9+() -]+") || phoneKey(phone).length() < 5))
            throw bad("联系电话格式不正确");
        return new CustomerInput(input.kind(), name, contact, phone);
    }
    static String phoneKey(String value) { return value.replaceAll("[^0-9]", ""); }

    List<Customer> matches(String kind, String name, String phone, long exclude) {
        String normalizedName = key(name), normalizedPhone = phoneKey(phone);
        return db.rows(Customer.class, """
            SELECT * FROM crm_customers WHERE id<>? AND
             ((?<>'' AND phone_key=?) OR (kind=? AND ?<>'' AND name_key=?))
            ORDER BY id
            """, exclude, normalizedPhone, normalizedPhone, kind, normalizedName, normalizedName);
    }
    List<Customer> duplicates(CustomerInput input, long exclude) {
        String phone = phoneKey(input.phone());
        return matches(input.kind(), input.name(), input.phone(), exclude).stream()
            .filter(c -> c.kind().equals("COMPANY") || input.kind().equals("COMPANY") ||
                phone.isEmpty() || c.phone().isEmpty() || phone.equals(phoneKey(c.phone())))
            .toList();
    }

    long insertCustomer(CustomerInput input, long owner) {
        return db.insert("""
            INSERT INTO crm_customers (kind,name,contact_name,phone,name_key,phone_key,created_by,created_at)
            VALUES (?,?,?,?,?,?,?,?)
            """, input.kind(), input.name(), input.contactName(), input.phone(), key(input.name()), phoneKey(input.phone()), owner, now());
    }

    @Transactional
    public CustomerView saveCustomer(Long id, CustomerInput input) {
        db.lock();
        var scope = access.business();
        var all = visibility(scope);
        var clean = clean(input);
        if (id != null && !manages(scope, all, visibleCustomer(scope, all, id))) throw denied();
        var duplicates = duplicates(clean, id == null ? 0 : id);
        if (!duplicates.isEmpty()) {
            if (duplicates.stream().anyMatch(c -> visible(scope, all, c)))
                throw conflict("已有疑似重复客户，请使用已有客户或请指定运营人员复核");
            throw conflict("发现疑似重复客户，请交指定运营人员核对并关联");
        }
        if (id == null) id = insertCustomer(clean, scope.id());
        else db.write("UPDATE crm_customers SET kind=?,name=?,contact_name=?,phone=?,name_key=?,phone_key=? WHERE id=?",
            clean.kind(), clean.name(), clean.contactName(), clean.phone(), key(clean.name()), phoneKey(clean.phone()), id);
        return view(scope, visibility(scope), customer(id));
    }

    List<CustomerView> reviewedMatches(Match match) {
        access.requiredPermission(CrmAccess.REVIEW);
        String name = optional(match.name(), 120, "客户名称"), phone = optional(match.phone(), 30, "联系电话");
        if (name.isBlank() && phoneKey(phone).length() < 5) throw bad("请填写准确的客户名称或联系电话");
        return matches(match.kind(), name, phone, 0).stream().map(c ->
            new CustomerView(c.id(), c.kind(), c.name(), c.contactName(), c.phone(), false, c.createdAt())).toList();
    }

    @Transactional
    public void assignMatch(long customerId, long userId, String reason) {
        db.lock();
        var scope = access.requiredPermission(CrmAccess.REVIEW);
        scope.active(userId);
        customer(customerId);
        String why = required(reason, 500, "核对原因");
        if (db.number("SELECT COUNT(*) FROM crm_customer_links WHERE customer_id=? AND user_id=?", customerId, userId) == 0) {
            db.write("INSERT INTO crm_customer_links (customer_id,user_id,linked_by,linked_at) VALUES (?,?,?,?)",
                customerId, userId, scope.id(), now());
            db.audit(scope.id(), "CUSTOMER_LINK", "customer=" + customerId + "; user=" + userId + "; " + why);
        }
    }

    ServiceRow service(long id) {
        return db.one(ServiceRow.class, "SELECT * FROM crm_services WHERE id=?", id).orElseThrow(CrmStore::missing);
    }
    private ServiceRow visibleService(Scope scope, Visibility all, long id) {
        var row = service(id);
        if (!visible(scope, all, row)) throw missing();
        return row;
    }
    private ServiceRow owned(Scope scope, long id) {
        var row = visibleService(scope, visibility(scope), id);
        if (row.ownerId() != scope.id()) throw denied();
        return row;
    }
    void checkCustomer(Scope scope, long id) { visibleCustomer(scope, visibility(scope), id); }
    ServiceRow ownedService(Scope scope, long id) { return owned(scope, id); }
    boolean canSeeService(Scope scope, long id) {
        var all = visibility(scope);
        return all.services().stream().anyMatch(s -> s.id() == id && visible(scope, all, s));
    }
    private boolean canTransfer(Scope scope, ServiceRow row) {
        var owner = scope.users().get(row.ownerId());
        return row.status().equals("PENDING") && (row.ownerId() == scope.id()
            || (owner != null && owner.isDisabled() && scope.may(CrmAccess.SERVICE)));
    }
    static String paymentStatus(long due, long net) {
        return due == 0 ? "NOT_APPLICABLE" : net <= 0 ? "UNPAID" : net < due ? "PARTIAL" : "PAID";
    }
    private ServiceView view(Scope scope, Visibility all, ServiceRow row, Customer customer) {
        var people = all.participants().getOrDefault(row.id(), Set.of()).stream().sorted()
            .map(id -> new Member(id, scope.name(id))).toList();
        return new ServiceView(row.id(), row.customerId(), customer.name(),
            new Member(row.ownerId(), scope.name(row.ownerId())), row.kind(), row.status(), row.scheduledAt(), row.actualAt(),
            row.description(), people, paymentStatus(row.receivableCents(), all.balances().getOrDefault(row.id(), 0L)),
            row.ownerId() == scope.id(), canTransfer(scope, row), "COMPLETED".equals(row.status()) && scope.may(CrmAccess.SERVICE));
    }
    @Transactional(readOnly = true)
    public List<ServiceView> services(String status, Long employee) {
        var scope = access.business();
        var all = visibility(scope);
        var customers = new HashMap<Long, Customer>();
        return all.services().stream().filter(row -> visible(scope, all, row))
            .filter(row -> status == null || status.isBlank() || status.equals(row.status()))
            .filter(row -> employee == null || employee == row.ownerId() || all.participants().getOrDefault(row.id(), Set.of()).contains(employee))
            .map(row -> view(scope, all, row, customers.computeIfAbsent(row.customerId(), this::customer))).toList();
    }
    @Transactional(readOnly = true)
    public ServiceDetail serviceDetail(long id) { return serviceDetail(access.business(), id); }

    private ServiceDetail serviceDetail(Scope scope, long id) {
        var all = visibility(scope);
        var row = visibleService(scope, all, id);
        var events = db.rows(Event.class, "SELECT * FROM crm_service_events WHERE service_id=? ORDER BY id DESC", id).stream()
            .map(e -> new EventView(e.id(), scope.name(e.actorId()), e.eventType(), e.detail(), e.recordedAt())).toList();
        var links = db.values(Long.class, "SELECT task_id FROM crm_service_tasks WHERE service_id=? ORDER BY task_id", id).stream()
            .flatMap(taskId -> tasks.readableDetail(taskId).stream()).map(t -> new TaskLink(t.id(), t.title())).toList();
        return new ServiceDetail(view(scope, all, row, customer(row.customerId())), events, links);
    }
    private void event(long id, Scope scope, String type, String text) {
        db.write("INSERT INTO crm_service_events (service_id,actor_id,event_type,detail,recorded_at) VALUES (?,?,?,?,?)",
            id, scope.id(), type, text, now());
    }
    private Set<Long> participants(Scope scope, Long serviceId, Set<Long> ids) {
        var people = ids == null ? Set.<Long>of() : ids;
        if (people.size() > 30 || people.stream().anyMatch(Objects::isNull)) throw bad("最多选择 30 位有效参与人");
        var prior = serviceId == null ? List.<Long>of() :
            db.values(Long.class, "SELECT user_id FROM crm_service_members WHERE service_id=?", serviceId);
        people.stream().filter(id -> !prior.contains(id)).forEach(scope::active);
        return people;
    }
    private void saveParticipants(long id, Set<Long> people) {
        for (long person : people) db.write("INSERT INTO crm_service_members (service_id,user_id) VALUES (?,?)", id, person);
    }
    private static LocalDateTime scheduled(LocalDateTime value) {
        if (value == null || value.getYear() < 2000 || value.getYear() > 2099) throw bad("请选择有效的服务时间");
        return value.withNano(0);
    }
    private static LocalDateTime occurred(LocalDateTime value) {
        value = value == null ? now() : scheduled(value);
        if (value.isAfter(now())) throw bad("实际服务时间不能晚于现在");
        return value;
    }
    @Transactional
    public ServiceDetail saveService(Long id, ServiceInput input) {
        db.lock();
        var scope = access.business();
        if (input.customerId() == null) throw bad("请选择客户");
        visibleCustomer(scope, visibility(scope), input.customerId());
        if (input.kind() == null || !SERVICE_KINDS.contains(input.kind())) throw bad("请选择业务类型");
        var date = scheduled(input.scheduledAt());
        var previous = id == null ? null : owned(scope, id);
        if (previous != null && !previous.status().equals("PENDING")) throw conflict("只有待服务记录可以编辑");
        var people = participants(scope, id, input.participantIds());
        String text = required(input.description(), 2000, "服务说明");
        if (id == null) {
            id = db.insert("""
                INSERT INTO crm_services (customer_id,owner_id,kind,status,scheduled_at,description,created_at)
                VALUES (?,?,?,'PENDING',?,?,?)
                """, input.customerId(), scope.id(), input.kind(), date, text, now());
            event(id, scope, "CREATED", "登记服务");
        } else {
            if (previous.customerId() != input.customerId()
                && db.number("SELECT COUNT(*) FROM crm_cash WHERE service_id=?", id) > 0)
                throw conflict("已有收退款流水，不能更换客户");
            db.write("UPDATE crm_services SET customer_id=?,kind=?,scheduled_at=?,description=? WHERE id=?",
                input.customerId(), input.kind(), date, text, id);
            db.write("DELETE FROM crm_service_members WHERE service_id=?", id);
            event(id, scope, "UPDATED", "修改服务安排");
        }
        saveParticipants(id, people);
        return serviceDetail(scope, id);
    }
    @Transactional
    public ServiceDetail status(long id, ChangeStatus input) {
        db.lock();
        var scope = access.business();
        var row = owned(scope, id);
        if (!row.status().equals("PENDING")) throw conflict("服务已结束，不能重复提交状态");
        boolean completed = "COMPLETED".equals(input.status());
        if (!completed && !"CANCELLED".equals(input.status())) throw bad("只能完成或取消待服务记录");
        db.write("UPDATE crm_services SET status=?,actual_at=? WHERE id=?", input.status(), completed ? occurred(input.actualAt()) : null, id);
        event(id, scope, completed ? "COMPLETED" : "CANCELLED", completed ? "完成服务" : "取消服务");
        return serviceDetail(scope, id);
    }
    @Transactional
    public void transfer(long id, Transfer input) {
        db.lock();
        var scope = access.business();
        var row = visibleService(scope, visibility(scope), id);
        if (!canTransfer(scope, row)) throw conflict("该服务不能转交");
        if (input.ownerId() == null || input.ownerId() == row.ownerId()) throw bad("请选择新的负责人");
        scope.active(input.ownerId());
        String reason = required(input.reason(), 500, "转交原因");
        db.write("UPDATE crm_services SET owner_id=? WHERE id=?", input.ownerId(), id);
        event(id, scope, "TRANSFERRED", scope.name(row.ownerId()) + " → " + scope.name(input.ownerId()) + "：" + reason);
        notifications.notifyReference(input.ownerId(), "crm-service-transfer", "有服务转交给你",
            "请打开详情查看并跟进服务。", id);
    }
    @Transactional
    public ServiceDetail addNote(long id, String note) {
        db.lock();
        var scope = access.business();
        owned(scope, id);
        event(id, scope, "NOTE", required(note, 2000, "跟进备注"));
        return serviceDetail(scope, id);
    }
    @Transactional
    public ServiceDetail linkTask(long id, long taskId) {
        db.lock();
        var scope = access.business();
        owned(scope, id);
        if (tasks.readableDetail(taskId).isEmpty()) throw missing();
        if (db.number("SELECT COUNT(*) FROM crm_service_tasks WHERE service_id=? AND task_id=?", id, taskId) == 0) {
            db.write("INSERT INTO crm_service_tasks (service_id,task_id) VALUES (?,?)", id, taskId);
            event(id, scope, "TASK_LINKED", "关联了跟进任务");
        }
        return serviceDetail(scope, id);
    }
    @Transactional
    public void correct(long id, Correction input) {
        db.lock();
        var scope = access.requiredPermission(CrmAccess.SERVICE);
        var all = visibility(scope);
        var row = visibleService(scope, all, id);
        if (!row.status().equals("COMPLETED")) throw conflict("仅能更正已完成的服务");
        if (input.actualAt() == null) throw bad("请填写实际服务时间");
        var date = occurred(input.actualAt());
        String text = required(input.description(), 2000, "服务说明");
        String reason = required(input.reason(), 400, "更正原因");
        long customer = input.customerId() == null ? row.customerId() : input.customerId();
        long owner = input.ownerId() == null ? row.ownerId() : input.ownerId();
        if (owner != row.ownerId()) scope.active(owner);
        boolean changedCustomer = customer != row.customerId();
        if (changedCustomer) visibleCustomer(scope, all, customer);
        if (date.equals(row.actualAt()) && text.equals(row.description()) && !changedCustomer && owner == row.ownerId()) return;
        if ((changedCustomer || !date.equals(row.actualAt())) && db.number("SELECT COUNT(*) FROM crm_card_uses WHERE service_id=?", id) > 0)
            throw conflict("已登记年卡使用的服务不能更改客户或实际时间");
        if (changedCustomer && db.number("SELECT COUNT(*) FROM crm_cash WHERE service_id=?", id) > 0)
            throw conflict("已有收退款流水，不能更换客户");
        event(id, scope, "CORRECTED", "原因：" + reason + "\n原客户编号：" + row.customerId() + "\n原负责人编号：" + row.ownerId()
            + "\n原实际时间：" + row.actualAt() + "\n原说明：" + row.description());
        db.write("UPDATE crm_services SET customer_id=?,owner_id=?,actual_at=?,description=? WHERE id=?", customer, owner, date, text, id);
    }
}
