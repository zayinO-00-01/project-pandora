ALTER TABLE work_logs ADD COLUMN revision BIGINT NOT NULL DEFAULT 0;

CREATE TABLE personal_todo_requests (
    owner_id BIGINT NOT NULL,
    request_id VARCHAR(36) NOT NULL,
    todo_id VARCHAR(36) NOT NULL,
    PRIMARY KEY (owner_id, request_id),
    CONSTRAINT fk_todo_request_owner FOREIGN KEY (owner_id) REFERENCES users(id)
);
