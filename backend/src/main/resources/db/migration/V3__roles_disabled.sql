ALTER TABLE users ADD COLUMN disabled BOOLEAN NOT NULL DEFAULT FALSE;

UPDATE users SET role = 'DEPT_HEAD' WHERE role = 'LEADER';

UPDATE users SET display_name = '部门老总' WHERE username = 'leader';

UPDATE users SET display_name = '系统管理员' WHERE username = 'admin';
