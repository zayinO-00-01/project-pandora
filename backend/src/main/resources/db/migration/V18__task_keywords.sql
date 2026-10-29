CREATE TABLE task_keywords (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    task_id BIGINT NOT NULL,
    created_by BIGINT NOT NULL,
    text VARCHAR(24) NOT NULL,
    CONSTRAINT fk_keyword_task FOREIGN KEY (task_id) REFERENCES dispatch_tasks(id),
    CONSTRAINT fk_keyword_author FOREIGN KEY (created_by) REFERENCES users(id)
);
CREATE INDEX idx_keyword_task ON task_keywords(task_id);
