-- password for all demo accounts: demo1234
INSERT INTO users (username, password_hash, role, display_name, manager_id) VALUES
('admin', '$2a$10$zjNfRmF0LG79cXxl5tAJ0./zZ0LjSK1ITK5qcDnGxcg.mJcUoUYcy', 'ADMIN', '管理员', NULL);

INSERT INTO users (username, password_hash, role, display_name, manager_id) VALUES
('leader', '$2a$10$zjNfRmF0LG79cXxl5tAJ0./zZ0LjSK1ITK5qcDnGxcg.mJcUoUYcy', 'LEADER', '部门领导', NULL);

INSERT INTO users (username, password_hash, role, display_name, manager_id)
SELECT 'staff', '$2a$10$zjNfRmF0LG79cXxl5tAJ0./zZ0LjSK1ITK5qcDnGxcg.mJcUoUYcy', 'STAFF', '员工小王', id
FROM users WHERE username = 'leader';
