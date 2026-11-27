package com.projectpandora.api.crm;

import static com.projectpandora.api.crm.CrmStore.bad;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/crm")
class CrmController {
    private final CrmAccess access;
    private final CrmWorkspace work;
    private final CrmCommerce commerce;
    private final CrmContracts contracts;

    CrmController(CrmAccess access, CrmWorkspace work, CrmCommerce commerce, CrmContracts contracts) {
        this.access = access;
        this.work = work;
        this.commerce = commerce;
        this.contracts = contracts;
    }

    record Text(String text) {}
    record Reason(String reason) {}
    record Task(Long taskId) {}
    record Use(Long serviceId) {}
    record Link(Long userId, String reason) {}
    record Grants(Set<String> permissions) {}

    @GetMapping("/capabilities")
    public CrmAccess.Capabilities capabilities() { return access.capabilities(); }
    @GetMapping("/members")
    public List<CrmAccess.Member> members() { return access.members(); }
    @GetMapping("/customers")
    public List<CrmWorkspace.CustomerView> customers(@RequestParam(required = false) String q) { return work.customers(q); }
    @PostMapping("/customers")
    @ResponseStatus(HttpStatus.CREATED)
    public CrmWorkspace.CustomerView customer(@RequestBody CrmWorkspace.CustomerInput input) { return work.saveCustomer(null, input); }
    @GetMapping("/customers/{id}")
    public CrmWorkspace.CustomerDetail customer(@PathVariable long id) { return work.detail(id); }
    @PutMapping("/customers/{id}")
    public CrmWorkspace.CustomerView customer(@PathVariable long id, @RequestBody CrmWorkspace.CustomerInput input) {
        return work.saveCustomer(id, input);
    }
    @PostMapping("/customer-matches")
    public List<CrmWorkspace.CustomerView> matches(@RequestBody CrmWorkspace.Match input) { return work.reviewedMatches(input); }
    @PutMapping("/customer-matches/{id}/assign")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void assign(@PathVariable long id, @RequestBody Link input) {
        if (input.userId() == null) throw bad("请选择员工");
        work.assignMatch(id, input.userId(), input.reason());
    }
    @GetMapping("/customers/{id}/orders")
    public List<CrmCommerce.OrderView> orders(@PathVariable long id) { return commerce.orders(id); }
    @GetMapping("/services")
    public List<CrmWorkspace.ServiceView> services(@RequestParam(required = false) String status,
                                                   @RequestParam(required = false) Long employeeId) {
        return work.services(status, employeeId);
    }
    @PostMapping("/services")
    @ResponseStatus(HttpStatus.CREATED)
    public CrmWorkspace.ServiceDetail service(@RequestBody CrmWorkspace.ServiceInput input) { return work.saveService(null, input); }
    @GetMapping("/services/{id}")
    public CrmWorkspace.ServiceDetail service(@PathVariable long id) { return work.serviceDetail(id); }
    @PutMapping("/services/{id}")
    public CrmWorkspace.ServiceDetail service(@PathVariable long id, @RequestBody CrmWorkspace.ServiceInput input) {
        return work.saveService(id, input);
    }
    @PutMapping("/services/{id}/status")
    public CrmWorkspace.ServiceDetail status(@PathVariable long id, @RequestBody CrmWorkspace.ChangeStatus input) {
        return work.status(id, input);
    }
    @PostMapping("/services/{id}/transfer")
    public Map<String, String> transfer(@PathVariable long id, @RequestBody CrmWorkspace.Transfer input) {
        work.transfer(id, input);
        return Map.of("message", "已转交");
    }
    @PostMapping("/services/{id}/notes")
    public CrmWorkspace.ServiceDetail note(@PathVariable long id, @RequestBody Text input) { return work.addNote(id, input.text()); }
    @PostMapping("/services/{id}/tasks")
    public CrmWorkspace.ServiceDetail link(@PathVariable long id, @RequestBody Task input) {
        if (input.taskId() == null) throw bad("请选择跟进任务");
        return work.linkTask(id, input.taskId());
    }
    @PostMapping("/services/{id}/corrections")
    public Map<String, String> correct(@PathVariable long id, @RequestBody CrmWorkspace.Correction input) {
        work.correct(id, input);
        return Map.of("message", "已更正");
    }
    @PostMapping("/orders/{id}/uses")
    public CrmCommerce.OrderView use(@PathVariable long id, @RequestBody Use input) {
        if (input.serviceId() == null) throw bad("请选择已完成的观演服务");
        return commerce.useCard(id, input.serviceId());
    }
    @PostMapping("/orders/{id}/uses/{useId}/reverse")
    public CrmCommerce.OrderView reverse(@PathVariable long id, @PathVariable long useId, @RequestBody Reason input) {
        return commerce.reverse(id, useId, input.reason());
    }
    @PostMapping("/admin/imports")
    public CrmCommerce.ImportResult importOrders(@RequestBody CrmCommerce.ImportInput input) { return commerce.importOrders(input); }
    @GetMapping("/admin/imports")
    public List<CrmCommerce.ImportEvent> imports() { return commerce.imports(); }
    @PostMapping("/admin/imports/matches")
    public List<CrmWorkspace.CustomerView> importMatches(@RequestBody CrmWorkspace.Match input) { return commerce.importMatches(input); }
    @GetMapping("/admin/revenue")
    public CrmCommerce.Revenue revenue(@RequestParam(required = false) String month) { return commerce.revenue(month); }
    @GetMapping("/admin/shop-cash")
    public List<CrmCommerce.ShopCash> shopCash(@RequestParam(required = false) String month) { return commerce.shopCash(month); }
    @GetMapping("/admin/receivables")
    public List<CrmCommerce.Receivable> receivables() { return commerce.receivables(); }
    @PutMapping("/admin/receivables/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void receivables(@PathVariable long id, @RequestBody CrmCommerce.ReceivableInput input) { commerce.receivable(id, input); }
    @GetMapping("/admin/receivables/{id}/cash")
    public List<CrmCommerce.CashLine> cash(@PathVariable long id) { return commerce.entries(id); }
    @PostMapping("/admin/receivables/{id}/cash")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cash(@PathVariable long id, @RequestBody CrmCommerce.CashInput input) { commerce.cash(id, input); }
    @GetMapping("/admin/staff")
    public List<CrmContracts.Staff> staff() { return contracts.staff(); }
    @PutMapping("/admin/staff/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void staff(@PathVariable long id, @RequestBody CrmContracts.StaffInput input) { contracts.saveStaff(id, input); }
    @GetMapping("/admin/contracts")
    public List<CrmContracts.Contract> contracts(@RequestParam(required = false) Long userId) { return contracts.list(userId); }
    @PostMapping(value = "/admin/contracts", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Long> archive(@RequestParam long userId, @RequestParam String title,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startOn,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endOn,
        @RequestParam MultipartFile file) throws IOException {
        return Map.of("id", contracts.archive(userId, title, startOn, endOn, file));
    }
    @PutMapping("/admin/contracts/{id}/dates")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void dates(@PathVariable long id, @RequestBody CrmContracts.Dates input) { contracts.updateDates(id, input); }
    @GetMapping("/admin/contracts/{id}/pdf")
    public ResponseEntity<byte[]> pdf(@PathVariable long id) {
        var pdf = contracts.pdf(id);
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF).cacheControl(CacheControl.noStore())
            .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(pdf.fileName(), StandardCharsets.UTF_8).build().toString())
            .body(pdf.pdfData());
    }
    @PostMapping("/admin/contract-reminders")
    public CrmContracts.ReminderResult remind() { return contracts.manualReminders(); }
    @GetMapping("/admin/permissions/{id}")
    public Set<String> grants(@PathVariable long id) { return access.inspect(id); }
    @PutMapping("/admin/permissions/{id}")
    public Set<String> grants(@PathVariable long id, @RequestBody Grants input) { return access.grant(id, input.permissions()); }
}
