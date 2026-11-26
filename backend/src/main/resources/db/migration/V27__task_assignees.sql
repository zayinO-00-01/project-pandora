CREATE TABLE task_assignees (
    task_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    PRIMARY KEY (task_id, user_id),
    CONSTRAINT fk_task_assignees_task FOREIGN KEY (task_id) REFERENCES dispatch_tasks(id),
    CONSTRAINT fk_task_assignees_user FOREIGN KEY (user_id) REFERENCES users(id)
);
CREATE INDEX idx_task_assignees_user ON task_assignees(user_id, task_id);
INSERT INTO task_assignees(task_id, user_id) SELECT id, assignee_id FROM dispatch_tasks;
