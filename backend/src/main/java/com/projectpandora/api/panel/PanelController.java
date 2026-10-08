package com.projectpandora.api.panel;

import com.projectpandora.api.log.WorkLogRepository;
import com.projectpandora.api.log.WorkLogResponse;
import com.projectpandora.api.security.AccessService;
import com.projectpandora.api.security.UserPrincipal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/panels")
public class PanelController {

    private final WorkLogRepository workLogRepository;
    private final AccessService accessService;

    public PanelController(WorkLogRepository workLogRepository, AccessService accessService) {
        this.workLogRepository = workLogRepository;
        this.accessService = accessService;
    }

    @GetMapping("/map")
    public PanelMapResponse map() {
        UserPrincipal me = accessService.currentUser();
        List<WorkLogResponse> todayLogs =
                workLogRepository
                        .findByUserIdAndLogDateOrderByCreatedAtDesc(me.getId(), LocalDate.now(java.time.ZoneId.of("Asia/Shanghai")))
                        .stream()
                        .map(WorkLogResponse::from)
                        .toList();
        return new PanelMapResponse(List.of(), List.of(), List.of(), todayLogs);
    }
}
