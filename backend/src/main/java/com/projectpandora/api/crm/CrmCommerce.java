package com.projectpandora.api.crm;

import static com.projectpandora.api.crm.CrmStore.*;
import com.projectpandora.api.common.ApiException;
import com.projectpandora.api.crm.CrmAccess.Scope;
import com.projectpandora.api.crm.CrmWorkspace.CustomerInput;
import java.math.BigDecimal;
import java.time.*;
import java.time.format.DateTimeParseException;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

@Service
class CrmCommerce {
    private static final int CARD_USES = 6;
    private final CrmStore db;
    private final CrmAccess access;
    private final CrmWorkspace work;

    CrmCommerce(CrmStore db, CrmAccess access, CrmWorkspace work) {
        this.db = db;
        this.access = access;
        this.work = work;
    }

    record Order(long id, String sourceCode, String memberNumber, String externalNumber, long customerId,
                 long ownerId, String product, long totalCents, LocalDate paidOn, long refundedCents, LocalDate lastRefundOn) {
        boolean fullyRefunded() { return paidOn != null && refundedCents == totalCents; }
        boolean annualCard() { return product.equals("ANNUAL_CARD") && paidOn != null; }
    }
    record CardUse(long id, long serviceId, LocalDateTime usedAt, LocalDateTime reversedAt, String reverseReason) {}
    record OrderView(long id, String source, String number, String product, String status,
                     LocalDate startsOn, LocalDate expiresOn, Integer remaining, boolean usable,
                     boolean mayReverse, List<CardUse> uses) {}
    record ImportInput(String csv, Map<String, Long> links, boolean apply) {}
    record ImportRow(int row, String memberKey, String orderNumber, String result, String message) {}
    record ImportResult(boolean committed, boolean ready, List<ImportRow> rows) {}
    private record Incoming(String source, String member, String number, String product, long total, long refund,
                            LocalDate paidOn, LocalDate refundedOn, CustomerInput customer, long owner) {
        String memberKey() { return source + ":" + member; }
    }
    record CashSummary(String kind, long receipts, long refunds) {}
    record Revenue(String month, long receipts, long refunds, long net, List<CashSummary> byKind) {}
    record Receivable(long id, String customerName, String kind, long receivableCents, long net) {}
    record CashLine(long id, String direction, long amountCents, LocalDate bookedOn, String note) {}
    record ShopCash(long id, String source, String orderNumber, String product, String direction, long amountCents, LocalDate bookedOn) {}
    record ReceivableInput(Long amountCents) {}
    record CashInput(String direction, Long amountCents, LocalDate bookedOn, String note) {}
    record ImportEvent(long id, String detail, LocalDateTime recordedAt) {}

    private static LocalDate date(String text) {
        if (text.isBlank()) return null;
        try { return LocalDate.parse(text); }
        catch (DateTimeParseException e) { throw bad("日期请使用 YYYY-MM-DD"); }
    }
    private static LocalDate bookingDate(LocalDate value) {
        if (value == null || value.getYear() < 2000 || value.isAfter(today())) throw bad("请输入有效日期，不能晚于今天");
        return value;
    }
    private static long cents(String text) {
        if (!text.matches("[0-9]{1,9}(\\.[0-9]{1,2})?")) throw bad("金额请填写最多两位小数的非负数");
        return money(new BigDecimal(text).movePointRight(2).longValueExact());
    }
    private static long money(Long value) {
        if (value == null || value < 0 || value > MAX_CENTS) throw bad("金额超出范围");
        return value;
    }
    private static String identifier(String value, int max) {
        if (value.length() > max || !value.matches("[a-zA-Z0-9_-]+")) throw bad("商城来源、会员编号、订单号只能含字母、数字、下划线或短横线");
        return value.toLowerCase(Locale.ROOT);
    }
    private Incoming parse(List<String> cols, Scope scope) {
        if (cols.size() != CrmCsv.HEADER.size()) throw bad("订单列数不正确");
        String product = cols.get(3), status = cols.get(4);
        if (!Set.of("MEMBERSHIP", "ANNUAL_CARD", "GENERAL").contains(product)) throw bad("商品类型不正确");
        long total = cents(cols.get(5)), refund = cols.get(6).isEmpty() ? 0 : cents(cols.get(6));
        var paid = date(cols.get(7));
        var refunded = date(cols.get(8));
        boolean appropriate = switch (status) {
            case "UNPAID" -> paid == null && refunded == null && refund == 0;
            case "PAID" -> paid != null && refunded == null && refund == 0;
            case "PARTIAL_REFUND" -> paid != null && refunded != null && refund > 0 && refund < total;
            case "REFUNDED" -> paid != null && refunded != null && refund == total;
            default -> false;
        };
        if (!appropriate || total == 0 || (refunded != null && refunded.isBefore(paid)))
            throw bad("请核对订单状态、付款日期、退款日期及金额");
        if (paid != null) bookingDate(paid);
        if (refunded != null) bookingDate(refunded);
        long owner;
        try { owner = Long.parseLong(cols.get(11)); }
        catch (NumberFormatException e) { throw bad("请输入有效的负责人编号"); }
        scope.active(owner);
        return new Incoming(identifier(cols.get(0), 40), identifier(cols.get(1), 80), identifier(cols.get(2), 80),
            product, total, refund, paid, refunded, CrmWorkspace.clean(new CustomerInput("PERSON", cols.get(9), "", cols.get(10))), owner);
    }

    @Transactional
    public ImportResult importOrders(ImportInput input) {
        db.lock();
        var scope = access.requiredPermission(CrmAccess.IMPORT);
        var records = CrmCsv.read(input.csv());
        var links = input.links() == null ? Map.<String, Long>of() : input.links();
        if (links.size() > 200 || links.values().stream().anyMatch(v -> v == null || v <= 0)) throw bad("客户关联无效");
        var rows = new ArrayList<ImportRow>();
        int index = 1;
        for (var cols : records) {
            index++;
            Incoming order = null;
            try {
                order = parse(cols, scope);
                rows.add(process(index, scope, order, links.get(order.memberKey())));
            } catch (ApiException e) {
                rows.add(new ImportRow(index, order == null ? "" : order.memberKey(),
                    cols.size() > 2 ? optional(cols.get(2).substring(0, Math.min(80, cols.get(2).length())), 80, "订单号") : "",
                    "ERROR", e.getMessage()));
            }
        }
        boolean ready = rows.stream().noneMatch(r -> r.result().equals("ERROR"));
        boolean commit = ready && input.apply();
        if (!commit) TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
        else {
            long added = rows.stream().filter(r -> r.result().equals("ADDED")).count();
            long updated = rows.stream().filter(r -> r.result().equals("UPDATED")).count();
            db.audit(scope.id(), "ORDER_IMPORT", "新增 " + added + " 条，更新 " + updated + " 条，跳过 " + (rows.size() - added - updated) + " 条");
        }
        return new ImportResult(commit, ready, rows);
    }
    private ImportRow process(int index, Scope scope, Incoming in, Long link) {
        var old = db.one(Order.class, "SELECT * FROM crm_orders WHERE source_code=? AND external_number=?",
            in.source(), in.number()).orElse(null);
        long previousRefund = old == null ? 0 : old.refundedCents();
        if (old != null) {
            if (!old.memberNumber().equals(in.member()) || !old.product().equals(in.product())
                || old.totalCents() != in.total() || old.ownerId() != in.owner()
                || (old.paidOn() != null && !old.paidOn().equals(in.paidOn()))
                || (link != null && old.customerId() != link)
                || in.refund() < previousRefund
                || (old.lastRefundOn() != null && (in.refundedOn() == null || in.refundedOn().isBefore(old.lastRefundOn())))
                || (in.refund() == previousRefund && !Objects.equals(old.lastRefundOn(), in.refundedOn())))
                throw conflict("与已导入订单冲突，请核对会员、商品、负责人和历史收退款");
            if (Objects.equals(old.paidOn(), in.paidOn()) && in.refund() == previousRefund)
                return new ImportRow(index, in.memberKey(), in.number(), "SKIPPED", "相同订单已存在");
        }
        Long customer = db.values(Long.class, "SELECT customer_id FROM crm_shop_members WHERE source_code=? AND member_number=?",
            in.source(), in.member()).stream().findFirst().orElse(null);
        if (customer != null && link != null && !customer.equals(link)) throw conflict("会员已关联客户，不能改绑");
        if (customer == null) {
            if (link != null) {
                var picked = work.customer(link);
                if (!picked.kind().equals("PERSON")) throw bad("商城会员只能关联个人档案");
                customer = link;
            } else {
                if (!work.duplicates(in.customer(), 0).isEmpty())
                    throw conflict("疑似已有档案，请核对后明确选择已有客户");
                customer = work.insertCustomer(in.customer(), in.owner());
            }
            db.write("INSERT INTO crm_shop_members (source_code,member_number,customer_id) VALUES (?,?,?)",
                in.source(), in.member(), customer);
        }
        long orderId;
        if (old == null) orderId = db.insert("""
            INSERT INTO crm_orders (source_code,member_number,external_number,customer_id,owner_id,product,
            total_cents,paid_on,refunded_cents,last_refund_on) VALUES (?,?,?,?,?,?,?,?,?,?)
            """, in.source(), in.member(), in.number(), customer, in.owner(), in.product(), in.total(), in.paidOn(), in.refund(), in.refundedOn());
        else {
            orderId = old.id();
            db.write("UPDATE crm_orders SET paid_on=?,refunded_cents=?,last_refund_on=? WHERE id=?",
                in.paidOn(), in.refund(), in.refundedOn(), orderId);
        }
        if (in.paidOn() != null && (old == null || old.paidOn() == null))
            entry(scope.id(), null, orderId, "RECEIPT", in.total(), in.paidOn(), "商城订单到账");
        if (in.refund() > previousRefund)
            entry(scope.id(), null, orderId, "REFUND", in.refund() - previousRefund, in.refundedOn(), "商城订单退款");
        return new ImportRow(index, in.memberKey(), in.number(), old == null ? "ADDED" : "UPDATED",
            old == null ? "新增订单" : "更新支付状态和流水");
    }

    List<CrmWorkspace.CustomerView> importMatches(CrmWorkspace.Match match) {
        access.requiredPermission(CrmAccess.IMPORT);
        String name = optional(match.name(), 120, "客户名称"), phone = optional(match.phone(), 30, "联系电话");
        if (name.isBlank() && CrmWorkspace.phoneKey(phone).length() < 5) throw bad("请填写准确的客户名称或联系电话");
        return work.matches("PERSON", name, phone, 0).stream().map(c ->
            new CrmWorkspace.CustomerView(c.id(), c.kind(), c.name(), c.contactName(), c.phone(), false, c.createdAt())).toList();
    }
    List<ImportEvent> imports() {
        var scope = access.requiredPermission(CrmAccess.IMPORT);
        return db.rows(ImportEvent.class,
            "SELECT id,detail,recorded_at FROM crm_audit WHERE event_type='ORDER_IMPORT' AND actor_id=? ORDER BY id DESC LIMIT 50", scope.id());
    }

    @Transactional(readOnly = true)
    public List<OrderView> orders(long customerId) {
        var scope = access.business();
        work.checkCustomer(scope, customerId);
        return db.rows(Order.class, "SELECT * FROM crm_orders WHERE customer_id=? ORDER BY id DESC", customerId).stream()
            .filter(o -> scope.maySeeOwner(o.ownerId())).map(o -> orderView(scope, o)).toList();
    }
    private boolean active(Order order) {
        return order.paidOn() != null && !order.fullyRefunded() && !today().isBefore(order.paidOn())
            && today().isBefore(order.paidOn().plusMonths(12));
    }
    private int remaining(long id) {
        return CARD_USES - (int) db.number("SELECT COUNT(*) FROM crm_card_uses WHERE order_id=? AND reversed_at IS NULL", id);
    }
    private OrderView orderView(Scope scope, Order order) {
        String status = order.paidOn() == null ? "UNPAID" : order.fullyRefunded() ? "REFUNDED"
            : order.refundedCents() > 0 ? "PARTIAL_REFUND" : "PAID";
        var expiry = order.paidOn() != null && !"GENERAL".equals(order.product()) ? order.paidOn().plusMonths(12) : null;
        var uses = order.annualCard() ? db.rows(CardUse.class, """
            SELECT id,service_id,used_at,reversed_at,reverse_reason FROM crm_card_uses WHERE order_id=? ORDER BY id DESC
            """, order.id()).stream().filter(u -> work.canSeeService(scope, u.serviceId())).toList() : List.<CardUse>of();
        Integer remaining = order.annualCard() ? remaining(order.id()) : null;
        return new OrderView(order.id(), order.sourceCode(), order.externalNumber(), order.product(), status, order.paidOn(),
            expiry, remaining, remaining != null && remaining > 0 && active(order), scope.may(CrmAccess.CARD), uses);
    }
    private Order accessibleOrder(Scope scope, long id) {
        return db.one(Order.class, "SELECT * FROM crm_orders WHERE id=?", id)
            .filter(o -> scope.maySeeOwner(o.ownerId())).orElseThrow(CrmStore::missing);
    }
    @Transactional
    public OrderView useCard(long orderId, long serviceId) {
        db.lock();
        var scope = access.business();
        var order = accessibleOrder(scope, orderId);
        var service = work.ownedService(scope, serviceId);
        if (!order.annualCard() || service.customerId() != order.customerId()
            || !service.kind().equals("SHOW") || !service.status().equals("COMPLETED"))
            throw bad("只可核销该客户已完成的观演服务");
        var eventDate = service.actualAt().toLocalDate();
        if (!active(order) || eventDate.isBefore(order.paidOn()) || !eventDate.isBefore(order.paidOn().plusMonths(12))
            || remaining(order.id()) < 1) throw conflict("年卡已过期、已停用、次数不足或服务时间不在有效期");
        if (db.number("SELECT COUNT(*) FROM crm_card_uses WHERE service_id=?", serviceId) != 0) throw conflict("该服务已有核销记录");
        db.write("INSERT INTO crm_card_uses (order_id,service_id,used_by,used_at) VALUES (?,?,?,?)",
            orderId, serviceId, scope.id(), now());
        return orderView(scope, order);
    }
    @Transactional
    public OrderView reverse(long orderId, long useId, String reason) {
        db.lock();
        var scope = access.requiredPermission(CrmAccess.CARD);
        var order = accessibleOrder(scope, orderId);
        var use = db.one(CardUse.class, """
            SELECT id,service_id,used_at,reversed_at,reverse_reason FROM crm_card_uses WHERE id=? AND order_id=?
            """, useId, orderId).orElseThrow(CrmStore::missing);
        if (!work.canSeeService(scope, use.serviceId())) throw missing();
        if (use.reversedAt() != null) throw conflict("这笔核销已经撤销");
        db.write("UPDATE crm_card_uses SET reversed_by=?,reversed_at=?,reverse_reason=? WHERE id=?",
            scope.id(), now(), required(reason, 500, "撤销原因"), useId);
        return orderView(scope, order);
    }

    @Transactional(readOnly = true)
    public Revenue revenue(String month) {
        if (!access.capabilities().revenue()) throw denied();
        var range = monthRange(month);
        var totals = db.rows(CashSummary.class, """
            SELECT COALESCE(s.kind,o.product) AS kind,
              SUM(CASE WHEN c.direction='RECEIPT' THEN c.amount_cents ELSE 0 END) AS receipts,
              SUM(CASE WHEN c.direction='REFUND' THEN c.amount_cents ELSE 0 END) AS refunds
            FROM crm_cash c LEFT JOIN crm_services s ON c.service_id=s.id LEFT JOIN crm_orders o ON c.order_id=o.id
            WHERE c.booked_on>=? AND c.booked_on<? GROUP BY COALESCE(s.kind,o.product) ORDER BY kind
            """, range.atDay(1), range.plusMonths(1).atDay(1));
        long receipts = totals.stream().mapToLong(CashSummary::receipts).sum(), refunds = totals.stream().mapToLong(CashSummary::refunds).sum();
        return new Revenue(range.toString(), receipts, refunds, receipts - refunds, totals);
    }
    private static YearMonth monthRange(String month) {
        YearMonth range;
        try { range = month == null || month.isBlank() ? YearMonth.from(today()) : YearMonth.parse(month); }
        catch (DateTimeParseException e) { throw bad("月份格式不正确"); }
        if (range.getYear() < 2000 || range.getYear() > 2099) throw bad("月份超出范围");
        return range;
    }
    List<ShopCash> shopCash(String month) {
        access.requiredPermission(CrmAccess.FINANCE);
        var range = monthRange(month);
        return db.rows(ShopCash.class, """
            SELECT c.id,o.source_code AS source,o.external_number AS order_number,o.product,c.direction,c.amount_cents,c.booked_on
            FROM crm_cash c JOIN crm_orders o ON c.order_id=o.id
            WHERE c.booked_on>=? AND c.booked_on<? ORDER BY c.booked_on DESC,c.id DESC
            """, range.atDay(1), range.plusMonths(1).atDay(1));
    }
    List<Receivable> receivables() {
        access.requiredPermission(CrmAccess.FINANCE);
        return db.rows(Receivable.class, """
            SELECT s.id,c.name AS customer_name,s.kind,s.receivable_cents,
               COALESCE((SELECT SUM(CASE WHEN f.direction='RECEIPT' THEN f.amount_cents ELSE -f.amount_cents END)
                 FROM crm_cash f WHERE f.service_id=s.id),0) AS net
            FROM crm_services s JOIN crm_customers c ON c.id=s.customer_id ORDER BY s.id DESC
            """);
    }
    List<CashLine> entries(long serviceId) {
        access.requiredPermission(CrmAccess.FINANCE);
        work.service(serviceId);
        return db.rows(CashLine.class, """
            SELECT id,direction,amount_cents,booked_on,note FROM crm_cash WHERE service_id=? ORDER BY booked_on DESC,id DESC
            """, serviceId);
    }
    private long net(long serviceId) {
        return db.number("""
            SELECT COALESCE(SUM(CASE WHEN direction='RECEIPT' THEN amount_cents ELSE -amount_cents END),0)
              FROM crm_cash WHERE service_id=?
            """, serviceId);
    }
    @Transactional
    public void receivable(long serviceId, ReceivableInput input) {
        db.lock();
        var scope = access.requiredPermission(CrmAccess.FINANCE);
        var service = work.service(serviceId);
        long amount = money(input.amountCents());
        if (amount < net(serviceId)) throw conflict("应收不能低于当前净实收");
        if (amount != service.receivableCents()) {
            db.write("UPDATE crm_services SET receivable_cents=? WHERE id=?", amount, serviceId);
            db.audit(scope.id(), "RECEIVABLE", "service=" + serviceId + "; " + service.receivableCents() + " -> " + amount);
        }
    }
    @Transactional
    public void cash(long serviceId, CashInput input) {
        db.lock();
        var scope = access.requiredPermission(CrmAccess.FINANCE);
        var service = work.service(serviceId);
        long amount = money(input.amountCents()), net = net(serviceId);
        if (amount == 0) throw bad("请输入大于零的金额");
        var date = bookingDate(input.bookedOn());
        var note = required(input.note(), 500, "款项说明");
        if ("RECEIPT".equals(input.direction())) {
            if (net + amount > service.receivableCents()) throw conflict("到账不能超过剩余应收");
        } else if ("REFUND".equals(input.direction())) {
            if (amount > net) throw conflict("退款不能超过当前实收");
            long balance = 0;
            for (var line : db.rows(CashLine.class, """
                SELECT id,direction,amount_cents,booked_on,note FROM crm_cash WHERE service_id=? ORDER BY booked_on,id
                """, serviceId)) {
                if (line.bookedOn().isAfter(date) && balance < amount) throw conflict("退款日期的实收余额不足");
                balance += line.direction().equals("RECEIPT") ? line.amountCents() : -line.amountCents();
            }
        } else throw bad("请选择收款或退款");
        entry(scope.id(), serviceId, null, input.direction(), amount, date, note);
    }
    private void entry(long actor, Long service, Long order, String direction, long amount, LocalDate date, String note) {
        db.write("""
            INSERT INTO crm_cash (service_id,order_id,direction,amount_cents,booked_on,actor_id,note)
            VALUES (?,?,?,?,?,?,?)
            """, service, order, direction, amount, date, actor, note);
    }
}
