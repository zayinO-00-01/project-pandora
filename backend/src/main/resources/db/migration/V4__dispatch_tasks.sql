CREATE TABLE dispatch_tasks (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(128) NOT NULL,
    detail TEXT NOT NULL,
    priority VARCHAR(16) NOT NULL DEFAULT 'normal',
    due_at TIMESTAMP(6) NULL,
    progress INT NOT NULL DEFAULT 0,
    progress_note TEXT NOT NULL,
    created_by BIGINT NOT NULL,
    assignee_id BIGINT NOT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_dispatch_tasks_creator FOREIGN KEY (created_by) REFERENCES users (id),
    CONSTRAINT fk_dispatch_tasks_assignee FOREIGN KEY (assignee_id) REFERENCES users (id),
    CONSTRAINT ck_dispatch_tasks_progress CHECK (progress >= 0 AND progress <= 100),
    CONSTRAINT ck_dispatch_tasks_priority CHECK (priority IN ('low', 'normal', 'high'))
);
CREATE INDEX idx_dispatch_tasks_assignee_created ON dispatch_tasks (assignee_id, created_at, id);
CREATE INDEX idx_dispatch_tasks_creator_created ON dispatch_tasks (created_by, created_at, id);
CREATE INDEX idx_dispatch_tasks_created ON dispatch_tasks (created_at, id);