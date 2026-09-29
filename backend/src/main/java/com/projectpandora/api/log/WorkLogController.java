package com.projectpandora.api.log;

import com.projectpandora.api.common.ApiException;
import com.projectpandora.api.security.AccessService;
import com.projectpandora.api.security.UserPrincipal;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/logs")
public class WorkLogController {

    private final WorkLogRepository workLogRepository;
    private final AccessService accessService;

    public WorkLogController(WorkLogRepository workLogRepository, AccessService accessService) {
        this.workLogRepository = workLogRepository;
        this.accessService = accessService;
    }

    @GetMapping("/integrity")
    public IntegrityResponse integrity() {
        UserPrincipal me = accessService.currentUser();
        return new IntegrityResponse(true, "完整性校验通过: user=" + me.getUsername());
    }

    @GetMapping
    public List<WorkLogResponse> list(
            @RequestParam(required = false) LocalDate date,
            @RequestParam(required = false) Long userId) {
        UserPrincipal me = accessService.currentUser();
        Long targetUserId = userId == null ? me.getId() : userId;
        accessService.assertCanViewUserLogs(targetUserId);
        List<WorkLogEntity> logs =
                date == null
                        ? workLogRepository.findByUserIdOrderByCreatedAtDesc(targetUserId)
                        : workLogRepository.findByUserIdAndLogDateOrderByCreatedAtDesc(
                                targetUserId, date);
        return logs.stream().map(WorkLogResponse::from).toList();
    }

    @PostMapping
    public ResponseEntity<WorkLogResponse> create(@Valid @RequestBody CreateLogRequest request) {
        if (request.content() == null || request.content().isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST.value(), "正文不能为空");
        }
        UserPrincipal me = accessService.currentUser();
        WorkLogEntity entity = new WorkLogEntity();
        entity.setUserId(me.getId());
        entity.setLogDate(request.logDate());
        entity.setContent(request.content().trim());
        entity.setStatus("submitted");
        workLogRepository.save(entity);
        return ResponseEntity.status(HttpStatus.CREATED).body(WorkLogResponse.from(entity));
    }
}
