package com.projectpandora.api.crm;

import static com.projectpandora.api.crm.CrmStore.*;
import com.projectpandora.api.notification.NotificationService;
import com.projectpandora.api.user.Role;
import com.projectpandora.api.user.UserEntity;
import java.io.IOException;
import java.time.LocalDate;
import java.util.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
class CrmContracts {
    private static final int MAX_PDF = 1_000_000;
    private static final byte[] PDF_MAGIC = {'%', 'P', 'D', 'F', '-'};
    private final CrmStore db;
    private final CrmAccess access;
    private final NotificationService notifications;

    CrmContracts(CrmStore db, CrmAccess access, NotificationService notifications) {
        this.db = db;
        this.access = access;
        this.notifications = notifications;
    }

    record StaffRow(long userId, String department, String jobTitle) {}
    record Staff(long id, String name, boolean disabled, String department, String jobTitle) {}
    record StaffInput(String department, String jobTitle) {}
    record Contract(long id, long userId, String title, LocalDate startOn, LocalDate endOn, String fileName) {}
    record Pdf(String fileName, byte[] pdfData) {}
    record Dates(LocalDate startOn, LocalDate endOn, String reason) {}
    record Recipient(long id) {}
    record ReminderResult(int sent) {}

    List<Staff> staff() {
        var scope = access.requiredPermission(CrmAccess.CONTRACTS);
        var profiles = new HashMap<Long, StaffRow>();
        db.rows(StaffRow.class, "SELECT * FROM crm_staff_profiles").forEach(s -> profiles.put(s.userId(), s));
        return scope.users().values().stream().filter(u -> u.getRole() != Role.ADMIN).sorted(Comparator.comparing(UserEntity::getId))
            .map(u -> {
                var custom = profiles.get(u.getId());
                return new Staff(u.getId(), scope.name(u.getId()), u.isDisabled(),
                    custom == null ? department(scope, u) : custom.department(), custom == null ? "" : custom.jobTitle());
            }).toList();
    }
    private String department(CrmAccess.Scope scope, UserEntity user) {
        var seen = new HashSet<Long>();
        for (var current = user; current != null && seen.add(current.getId()); current = scope.users().get(current.getManagerId()))
            if (current.getDepartmentName() != null && !current.getDepartmentName().isBlank()) return current.getDepartmentName();
        return "";
    }
    @Transactional
    public void saveStaff(long id, StaffInput input) {
        db.lock();
        var scope = access.requiredPermission(CrmAccess.CONTRACTS);
        employee(scope, id);
        var department = optional(input.department(), 64, "部门");
        var job = optional(input.jobTitle(), 64, "岗位");
        if (db.number("SELECT COUNT(*) FROM crm_staff_profiles WHERE user_id=?", id) == 0)
            db.write("INSERT INTO crm_staff_profiles (user_id,department,job_title) VALUES (?,?,?)", id, department, job);
        else db.write("UPDATE crm_staff_profiles SET department=?,job_title=? WHERE user_id=?", department, job, id);
        db.audit(scope.id(), "STAFF_PROFILE", "user=" + id);
    }
    private void employee(CrmAccess.Scope scope, long id) {
        var user = scope.users().get(id);
        if (user == null || user.getRole() == Role.ADMIN) throw bad("请选择员工");
    }
    List<Contract> list(Long employeeId) {
        var scope = access.requiredPermission(CrmAccess.CONTRACTS);
        if (employeeId == null) return db.rows(Contract.class, "SELECT id,user_id,title,start_on,end_on,file_name FROM crm_contracts ORDER BY end_on,id");
        employee(scope, employeeId);
        return db.rows(Contract.class, """
            SELECT id,user_id,title,start_on,end_on,file_name FROM crm_contracts WHERE user_id=? ORDER BY end_on,id
            """, employeeId);
    }
    Pdf pdf(long id) {
        access.requiredPermission(CrmAccess.CONTRACTS);
        return db.one(Pdf.class, "SELECT file_name,pdf_data FROM crm_contracts WHERE id=?", id).orElseThrow(CrmStore::missing);
    }
    private static void validateDates(LocalDate start, LocalDate end) {
        if (start == null || end == null || start.getYear() < 2000 || end.getYear() > 2099 || end.isBefore(start))
            throw bad("请填写有效的合同起止日期");
    }
    @Transactional
    public long archive(long userId, String rawTitle, LocalDate start, LocalDate end, MultipartFile file) throws IOException {
        db.lock();
        var scope = access.requiredPermission(CrmAccess.CONTRACTS);
        employee(scope, userId);
        var title = required(rawTitle, 120, "合同名称");
        validateDates(start, end);
        if (file.getSize() < 5 || file.getSize() > MAX_PDF) throw bad("请上传不超过 1 MB 的 PDF");
        byte[] contents = file.getBytes();
        if (!Arrays.equals(PDF_MAGIC, Arrays.copyOf(contents, PDF_MAGIC.length))) throw bad("请选择 PDF 文件");
        String filename = required(file.getOriginalFilename(), 180, "文件名");
        if (!filename.toLowerCase(Locale.ROOT).endsWith(".pdf") || filename.matches("(?s).*[\\x00-\\x1F\\\\/].*"))
            throw bad("请选择扩展名为 .pdf 的文件");
        long id = db.insert("""
            INSERT INTO crm_contracts (user_id,title,start_on,end_on,file_name,pdf_data,recorded_by) VALUES (?,?,?,?,?,?,?)
            """, userId, title, start, end, filename, contents, scope.id());
        db.audit(scope.id(), "CONTRACT_ARCHIVE", "contract=" + id);
        return id;
    }
    @Transactional
    public void updateDates(long id, Dates input) {
        db.lock();
        var scope = access.requiredPermission(CrmAccess.CONTRACTS);
        var before = db.one(Contract.class, "SELECT id,user_id,title,start_on,end_on,file_name FROM crm_contracts WHERE id=?", id)
            .orElseThrow(CrmStore::missing);
        validateDates(input.startOn(), input.endOn());
        String reason = required(input.reason(), 500, "更改原因");
        if (before.startOn().equals(input.startOn()) && before.endOn().equals(input.endOn())) return;
        db.write("UPDATE crm_contracts SET start_on=?,end_on=? WHERE id=?", input.startOn(), input.endOn(), id);
        db.audit(scope.id(), "CONTRACT_DATES", "contract=" + id + "; " + before.startOn() + "～" + before.endOn()
            + " -> " + input.startOn() + "～" + input.endOn() + "; " + reason);
    }
    @Transactional
    public ReminderResult manualReminders() {
        db.lock();
        access.requiredPermission(CrmAccess.CONTRACTS);
        return new ReminderResult(sendReminders());
    }
    @Scheduled(cron = "${pandora.crm.contract-reminders-cron:0 0 8 * * *}", zone = ZONE)
    @Transactional
    public void scheduledReminders() {
        db.lock();
        sendReminders();
    }
    private int sendReminders() {
        LocalDate day = today();
        var recipients = db.rows(Recipient.class, """
            SELECT u.id FROM crm_permissions p JOIN users u ON p.user_id=u.id
             WHERE p.permission_code=? AND u.disabled=FALSE AND u.role<>'ADMIN'
            """, CrmAccess.CONTRACTS);
        if (recipients.isEmpty()) return 0;
        var contracts = db.rows(Contract.class, """
            SELECT id,user_id,title,start_on,end_on,file_name FROM crm_contracts WHERE end_on>=? AND end_on<=?
            """, day, day.plusDays(30));
        int sent = 0;
        for (var contract : contracts) for (var recipient : recipients) {
            if (db.number("SELECT COUNT(*) FROM crm_contract_reminders WHERE contract_id=? AND end_on=? AND recipient_id=?",
                contract.id(), contract.endOn(), recipient.id()) > 0) continue;
            db.write("INSERT INTO crm_contract_reminders (contract_id,end_on,recipient_id) VALUES (?,?,?)",
                contract.id(), contract.endOn(), recipient.id());
            notifications.notifyUser(recipient.id(), "contract-reminder", "有员工合同即将到期",
                "请在 Web「员工与合同」检查待续签合同。", null);
            sent++;
        }
        return sent;
    }
}
