package com.projectpandora.api.task;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public record CreateTaskRequest(
        @NotBlank(message = "任务标题不能为空") @Size(max = 128, message = "任务标题不能超过128字") String title,
        @Size(max = 5000, message = "任务详情不能超过5000字") String detail,
        @Pattern(regexp = "low|normal|high", message = "优先级必须为 low、normal 或 high") String priority,
        @NotNull(message = "请选择责任人") Long assigneeId,
        Instant dueAt) {
    public CreateTaskRequest {
        title = title == null ? null : title.trim();
        detail = detail == null ? "" : detail;
        priority = priority == null ? "normal" : priority;
    }
}