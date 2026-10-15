package com.projectpandora.api.log;

import jakarta.validation.constraints.*;
import java.time.LocalDate;

public record CreateLogRequest(
    @NotNull(message="请选择日志日期") @PastOrPresent(message="日志日期不能晚于今天") LocalDate logDate,
    @NotBlank(message="正文不能为空") @Size(max=5000, message="正文不能超过5000字") String content,
    @Pattern(regexp="draft|submitted", message="状态必须为 draft 或 submitted") String status) {}
