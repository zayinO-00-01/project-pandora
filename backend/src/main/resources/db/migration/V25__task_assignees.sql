CREATE TABLE task_assignees (
 task_id BIGINT NOT NULL REFERENCES dispatch_tasks(id),
 user_id BIGINT NOT NULL REFERENCES users(id),
 PRIMARY KEY(task_id,user_id)
);
CREATE INDEX idx_task_assignees_user ON task_assignees(user_id,task_id);
INSERT INTO task_assignees(task_id,user_id) SELECT id,assignee_id FROM dispatch_tasks;
