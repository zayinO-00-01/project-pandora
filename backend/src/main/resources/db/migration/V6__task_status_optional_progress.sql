ALTER TABLE dispatch_tasks ADD COLUMN status VARCHAR(16) NOT NULL DEFAULT 'todo';
UPDATE dispatch_tasks SET status = CASE WHEN progress = 100 THEN 'done' WHEN progress = 0 THEN 'todo' ELSE 'doing' END;
ALTER TABLE dispatch_tasks MODIFY COLUMN progress INT NULL DEFAULT NULL;
ALTER TABLE dispatch_tasks ADD CONSTRAINT ck_dispatch_tasks_status CHECK (status IN ('todo', 'doing', 'done'));
ALTER TABLE dispatch_tasks ADD CONSTRAINT ck_dispatch_tasks_status_progress CHECK (
    progress IS NULL OR (status = 'todo' AND progress = 0) OR
    (status = 'doing' AND progress BETWEEN 0 AND 99) OR (status = 'done' AND progress = 100)
);
