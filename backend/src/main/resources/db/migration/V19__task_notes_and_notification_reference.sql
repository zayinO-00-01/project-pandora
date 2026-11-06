CREATE TABLE task_notes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    task_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    content VARCHAR(2000) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT uq_task_notes_task_user UNIQUE (task_id, user_id),
    CONSTRAINT fk_task_notes_task FOREIGN KEY (task_id) REFERENCES dispatch_tasks (id),
    CONSTRAINT fk_task_notes_user FOREIGN KEY (user_id) REFERENCES users (id)
);

ALTER TABLE notifications ADD COLUMN reference_id BIGINT NULL;
