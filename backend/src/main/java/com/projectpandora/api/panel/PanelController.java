package com.projectpandora.api.panel;

import com.projectpandora.api.log.WorkLogRepository;
import com.projectpandora.api.log.WorkLogResponse;
import com.projectpandora.api.security.AccessService;
import com.projectpandora.api.security.UserPrincipal;
import com.projectpandora.api.task.TaskService;
import java.util.stream.IntStream;
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
    private final TaskService taskService;

    public PanelController(WorkLogRepository workLogRepository, AccessService accessService, TaskService taskService) {
        this.workLogRepository = workLogRepository;
        this.accessService = accessService;
        this.taskService = taskService;
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
        var tasks = taskService.newest(10);
        var dispatch = IntStream.range(0, tasks.size())
                .mapToObj(i -> new PanelItemResponse(tasks.get(i).id(), tasks.get(i).title(), i, null))
                .toList();
        return new PanelMapResponse(List.of(), dispatch, List.of(), todayLogs);
    }
}
