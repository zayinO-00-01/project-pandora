CREATE TABLE personal_todos (id VARCHAR(36) PRIMARY KEY, owner_id BIGINT NOT NULL REFERENCES users(id), title VARCHAR(200) NOT NULL, todo_date DATE NOT NULL, completed BOOLEAN NOT NULL DEFAULT FALSE);
CREATE INDEX idx_personal_todos_owner_date ON personal_todos(owner_id,todo_date);
