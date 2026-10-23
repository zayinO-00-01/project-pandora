package com.projectpandora.api.task;

import java.time.Instant;

/** Serialized into dispatch_tasks.pending_change while a definition change awaits review. */
public record TaskChangePayload(String title, String detail, String priority, Instant dueAt) {}
