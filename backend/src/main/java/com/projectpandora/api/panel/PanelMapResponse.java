package com.projectpandora.api.panel;

import com.projectpandora.api.log.WorkLogResponse;
import java.util.List;

public record PanelMapResponse(
        List<PanelItemResponse> companyImportant,
        List<PanelItemResponse> companyDispatch,
        List<PanelItemResponse> personalTop,
        List<WorkLogResponse> todayLogs) {}
