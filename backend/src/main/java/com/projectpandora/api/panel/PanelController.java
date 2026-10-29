package com.projectpandora.api.panel;

import com.projectpandora.api.log.WorkLogRepository;
import com.projectpandora.api.log.WorkLogResponse;
import com.projectpandora.api.notice.CompanyNoticeService;
import com.projectpandora.api.security.AccessService;
import com.projectpandora.api.priority.PersonalPriorityService;
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
    private final CompanyNoticeService companyNoticeService;
    private final PersonalPriorityService personalPriorityService;

    public PanelController(WorkLogRepository workLogRepository, AccessService accessService, TaskService taskService,
                           CompanyNoticeService companyNoticeService, PersonalPriorityService personalPriorityService) {
        this.workLogRepository = workLogRepository;
        this.accessService = accessService;
        this.taskService = taskService;
        this.companyNoticeService = companyNoticeService;
        this.personalPriorityService = personalPriorityService;
    }

    @GetMapping("/map")
    public PanelMapResponse map() {
        UserPrincipal me = accessService.assertBusiness();
        List<WorkLogResponse> todayLogs =
                workLogRepository
                        .coveringDate(me.getId(), LocalDate.now(java.time.ZoneId.of("Asia/Shanghai")))
                        .stream()
                        .map(WorkLogResponse::from)
                        .toList();
        var important = companyNoticeService.enabled().stream()
                .map(notice -> new PanelItemResponse(notice.getId(), notice.getTitle(), notice.getSortOrder(),
                        companyNoticeService.noteOf(notice.getId(), me.getId()), notice.getContent()))
                .toList();
        var tasks = taskService.newest(10);
        var dispatch = IntStream.range(0, tasks.size())
                .mapToObj(i -> new PanelItemResponse(tasks.get(i).id(), tasks.get(i).title(), i, null))
                .toList();
        return new PanelMapResponse(important, dispatch, personalPriorityService.get(me.getId()).items(), todayLogs);
    }
}
