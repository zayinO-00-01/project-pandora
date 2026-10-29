ALTER TABLE dispatch_tasks ADD COLUMN approval_status VARCHAR(16) NOT NULL DEFAULT 'approved';
ALTER TABLE dispatch_tasks ADD COLUMN approval_note VARCHAR(500) NOT NULL DEFAULT '';
ALTER TABLE dispatch_tasks ADD COLUMN reviewer_id BIGINT NULL;
ALTER TABLE dispatch_tasks ADD COLUMN reviewed_at TIMESTAMP(6) NULL;
ALTER TABLE dispatch_tasks ADD CONSTRAINT ck_dispatch_tasks_approval CHECK (approval_status IN ('pending', 'approved', 'rejected'));
ALTER TABLE dispatch_tasks ADD CONSTRAINT fk_dispatch_tasks_reviewer FOREIGN KEY (reviewer_id) REFERENCES users (id);
CREATE INDEX idx_dispatch_tasks_approval_assignee ON dispatch_tasks (approval_status, assignee_id, created_at, id);
