CREATE TABLE work_labels (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(64) NOT NULL,
    color VARCHAR(7) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE
);
ALTER TABLE dispatch_tasks ADD COLUMN label_id BIGINT NULL;
ALTER TABLE dispatch_tasks ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE work_logs ADD COLUMN label_id BIGINT NULL;
ALTER TABLE dispatch_tasks ADD CONSTRAINT fk_dispatch_task_label FOREIGN KEY (label_id) REFERENCES work_labels(id);
ALTER TABLE work_logs ADD CONSTRAINT fk_work_log_label FOREIGN KEY (label_id) REFERENCES work_labels(id);
CREATE TABLE task_reviews (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    task_id BIGINT NOT NULL,
    applicant_id BIGINT NOT NULL,
    reviewer_id BIGINT NOT NULL,
    operation VARCHAR(16) NOT NULL,
    payload TEXT NOT NULL,
    status VARCHAR(16) NOT NULL,
    base_version BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    decided_at TIMESTAMP NULL,
    CONSTRAINT fk_task_review_task FOREIGN KEY (task_id) REFERENCES dispatch_tasks(id),
    CONSTRAINT fk_task_review_applicant FOREIGN KEY (applicant_id) REFERENCES users(id),
    CONSTRAINT fk_task_review_reviewer FOREIGN KEY (reviewer_id) REFERENCES users(id)
);
CREATE INDEX idx_review_pending ON task_reviews(task_id,status);
