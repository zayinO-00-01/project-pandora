CREATE TABLE personal_todo_labels (
    todo_id VARCHAR(36) NOT NULL REFERENCES personal_todos(id) ON DELETE CASCADE,
    label_id BIGINT NOT NULL REFERENCES work_labels(id),
    sort_order INT NOT NULL,
    PRIMARY KEY(todo_id, label_id)
);

INSERT INTO work_labels(name,color,active)
SELECT '计划','#BCD3EF',TRUE WHERE NOT EXISTS (SELECT 1 FROM work_labels WHERE name='计划');
INSERT INTO work_labels(name,color,active)
SELECT '跟进','#B9DAD0',TRUE WHERE NOT EXISTS (SELECT 1 FROM work_labels WHERE name='跟进');
INSERT INTO work_labels(name,color,active)
SELECT '提醒','#F3CCA8',TRUE WHERE NOT EXISTS (SELECT 1 FROM work_labels WHERE name='提醒');
