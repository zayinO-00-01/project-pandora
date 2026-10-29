package com.projectpandora.api.task;

import java.time.Instant;

public record TaskNoteResponse(Long taskId,String content,Instant updatedAt) {}
