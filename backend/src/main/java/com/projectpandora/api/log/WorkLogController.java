package com.projectpandora.api.log;

import com.projectpandora.api.common.ApiException;
import com.projectpandora.api.security.AccessService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/logs")
public class WorkLogController {
    private final WorkLogRepository logs;
    private final AccessService access;
    public WorkLogController(WorkLogRepository logs, AccessService access) {
        this.logs=logs; this.access=access;
    }
    @GetMapping("/integrity")
    public IntegrityResponse integrity() {
        return new IntegrityResponse(true, "完整性校验通过: user="+access.currentUser().getUsername());
    }
    @GetMapping
    public List<WorkLogResponse> list(@RequestParam(required=false) LocalDate date,
                                     @RequestParam(required=false) Long userId) {
        Long me=access.currentUser().getId();
        Long target=userId==null ? me : userId;
        access.assertCanViewUserLogs(target);
        var result=date==null ? logs.findByUserIdOrderByCreatedAtDesc(target)
                             : logs.findByUserIdAndLogDateOrderByCreatedAtDesc(target,date);
        return result.stream().filter(e -> me.equals(target) || "submitted".equals(e.getStatus()))
                     .map(WorkLogResponse::from).toList();
    }
    @PostMapping
    public ResponseEntity<WorkLogResponse> create(@Valid @RequestBody CreateLogRequest r) {
        var e=new WorkLogEntity();
        e.setUserId(access.currentUser().getId()); e.setLogDate(r.logDate());
        e.setContent(r.content().trim()); e.setStatus(r.status()==null ? "submitted" : r.status());
        return ResponseEntity.status(201).body(WorkLogResponse.from(logs.saveAndFlush(e)));
    }
    @PutMapping("/{id}")
    @Transactional
    public WorkLogResponse update(@PathVariable Long id, @Valid @RequestBody CreateLogRequest r) {
        var e=authorLog(id);
        if("submitted".equals(e.getStatus()) && "draft".equals(r.status()))
            throw new ApiException(400,"已提交日志不能改回草稿");
        e.setLogDate(r.logDate()); e.setContent(r.content().trim());
        if(r.status()!=null) e.setStatus(r.status());
        return WorkLogResponse.from(logs.saveAndFlush(e));
    }
    @PostMapping("/{id}/submit")
    @Transactional
    public WorkLogResponse submit(@PathVariable Long id) {
        var e=authorLog(id); e.setStatus("submitted");
        return WorkLogResponse.from(logs.saveAndFlush(e));
    }
    private WorkLogEntity authorLog(Long id) {
        var e=logs.findById(id).orElseThrow(() -> new ApiException(404,"日志不存在"));
        if(!e.getUserId().equals(access.currentUser().getId()))
            throw new ApiException(403,"只能修改或提交自己的日志");
        return e;
    }
}
