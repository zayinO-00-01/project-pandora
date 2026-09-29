package com.projectpandora.api.log;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record CreateLogRequest(@NotNull LocalDate logDate, @NotBlank String content) {}
