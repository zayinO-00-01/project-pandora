ALTER TABLE dispatch_tasks ADD COLUMN review_status VARCHAR(24) NOT NULL DEFAULT 'none';
ALTER TABLE dispatch_tasks ADD COLUMN review_reason TEXT NULL;
ALTER TABLE dispatch_tasks ADD COLUMN reviewed_by BIGINT NULL;
ALTER TABLE dispatch_tasks ADD COLUMN lifecycle_reviewed_at TIMESTAMP(6) NULL;
ALTER TABLE dispatch_tasks ADD COLUMN pending_change TEXT NULL;
ALTER TABLE dispatch_tasks ADD COLUMN deleted_at TIMESTAMP(6) NULL;
ALTER TABLE dispatch_tasks ADD COLUMN deleted_by BIGINT NULL;

CREATE TABLE task_events (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    task_id BIGINT NOT NULL,
    event_type VARCHAR(32) NOT NULL,
    actor_id BIGINT NOT NULL,
    note TEXT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_task_events_task FOREIGN KEY (task_id) REFERENCES dispatch_tasks (id),
    CONSTRAINT fk_task_events_actor FOREIGN KEY (actor_id) REFERENCES users (id)
);
CREATE INDEX idx_task_events_task ON task_events (task_id, id);
