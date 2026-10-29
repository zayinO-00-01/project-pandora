package com.projectpandora.api.task;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record TaskApprovalRequest(
        @NotBlank(message = "请选择审批结果")
        @Pattern(regexp = "approved|rejected", message = "审批结果必须为 approved 或 rejected") String decision,
        @Size(max = 500, message = "审批说明不能超过500字") String note) {
    public TaskApprovalRequest {
        note = note == null ? "" : note;
    }
}
