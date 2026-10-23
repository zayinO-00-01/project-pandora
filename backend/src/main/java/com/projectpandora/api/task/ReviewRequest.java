package com.projectpandora.api.task;

import jakarta.validation.constraints.Size;

/** Review decision note. Required as the reason when rejecting. */
public record ReviewRequest(@Size(max = 500, message = "说明不能超过500字") String note) {
    public ReviewRequest {
        note = note == null ? null : note.trim();
    }
}
