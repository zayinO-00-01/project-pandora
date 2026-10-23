package com.projectpandora.api.log;

import com.projectpandora.api.common.ApiException;
import com.projectpandora.api.security.AccessService;
import jakarta.validation.Valid;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/logs")
public class WorkLogController {
    private final WorkLogRepository logs;
    private final AccessService access;
    public WorkLogController(WorkLogRepository logs,AccessService access) {this.logs=logs;this.access=access;}
    @GetMapping("/integrity")
    public IntegrityResponse integrity() {
        return new IntegrityResponse(true, "完整性校验通过: user="+access.assertBusiness().getUsername());
    }
    @GetMapping
    public List<WorkLogResponse> list(@RequestParam(required=false) LocalDate date,
                                     @RequestParam(required=false) Long userId) {
        Long me=access.assertBusiness().getId();
        Long target=userId==null ? me : userId;
        access.assertCanViewUserLogs(target);
        var result=date==null ? logs.findByUserIdOrderByCreatedAtDesc(target) : logs.findByUserIdAndLogDateOrderByCreatedAtDesc(target,date);
        return result.stream().filter(e->me.equals(target) || "submitted".equals(e.getStatus())).map(WorkLogResponse::from).toList();
    }
    @PostMapping
    public ResponseEntity<WorkLogResponse> create(@Valid @RequestBody CreateLogRequest r) {
        assertCreatable(r.logDate());
        var e=new WorkLogEntity();
        e.setUserId(access.assertBusiness().getId()); e.setLogDate(r.logDate());
        e.setContent(r.content().trim()); e.setStatus(r.status()==null ? "submitted" : r.status());
        return ResponseEntity.status(201).body(WorkLogResponse.from(logs.saveAndFlush(e)));
    }
    @PutMapping("/{id}")
    @Transactional
    public WorkLogResponse update(@PathVariable Long id, @Valid @RequestBody CreateLogRequest r) {
        var e=authorLog(id);
        assertModifiable(e,403);
        assertCreatable(r.logDate());
        if("submitted".equals(e.getStatus()) && "draft".equals(r.status()))
            throw new ApiException(400,"已提交日志不能改回草稿");
        e.setLogDate(r.logDate()); e.setContent(r.content().trim());
        if(r.status()!=null) e.setStatus(r.status());
        return WorkLogResponse.from(logs.saveAndFlush(e));
    }
    @PostMapping("/{id}/submit")
    @Transactional
    public WorkLogResponse submit(@PathVariable Long id) {
        var e=authorLog(id); assertModifiable(e,403); e.setStatus("submitted");
        return WorkLogResponse.from(logs.saveAndFlush(e));
    }
    private WorkLogEntity authorLog(Long id) {
        var e=logs.findById(id).orElseThrow(() -> new ApiException(404,"日志不存在"));
        if(!e.getUserId().equals(access.assertBusiness().getId()))
            throw new ApiException(403,"只能修改或提交自己的日志");
        return e;
    }
    /** New entries may only target today, yesterday or the day before (Shanghai dates). */
    private void assertCreatable(LocalDate date) {
        var today=LocalDate.now(ZoneId.of("Asia/Shanghai"));
        if(date.isBefore(today.minusDays(2)) || date.isAfter(today))
            throw new ApiException(400,"只能新建今天、昨天和前天的日报");
    }
    /** Editable within 24 hours of creation, regardless of which day the entry is dated. */
    private void assertModifiable(WorkLogEntity e,int status) {
        var createdAt=e.getCreatedAt();
        if(createdAt==null || createdAt.isBefore(Instant.now().minus(24,java.time.temporal.ChronoUnit.HOURS)))
            throw new ApiException(status,"仅能修改 24 小时内创建的日报，历史日报仅供查看");
    }
}
