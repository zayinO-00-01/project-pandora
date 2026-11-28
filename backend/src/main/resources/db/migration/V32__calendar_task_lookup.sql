CREATE INDEX idx_dispatch_task_calendar ON dispatch_tasks (approval_status, deleted_at, due_at);
