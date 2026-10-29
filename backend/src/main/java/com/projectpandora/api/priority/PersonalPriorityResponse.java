package com.projectpandora.api.priority;

import com.projectpandora.api.panel.PanelItemResponse;
import java.util.List;

public record PersonalPriorityResponse(long revision, List<PanelItemResponse> items) {}
