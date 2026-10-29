package com.projectpandora.api.priority;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;

public record PersonalPriorityRequest(
        @NotNull @PositiveOrZero Long revision,
        @NotNull @Size(max = 10) List<@NotNull @Valid Item> items) {
    public record Item(Long id, @NotBlank String title, @Size(max = 2000) String detail, Integer sortOrder) {}
}
