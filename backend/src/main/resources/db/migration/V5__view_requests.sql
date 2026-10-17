CREATE TABLE view_requests (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    requester_id BIGINT NOT NULL,
    target_id BIGINT NOT NULL,
    reason VARCHAR(500) NOT NULL,
    status VARCHAR(16) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_view_requester FOREIGN KEY (requester_id) REFERENCES users(id),
    CONSTRAINT fk_view_target FOREIGN KEY (target_id) REFERENCES users(id),
    CONSTRAINT uq_view_pair UNIQUE (requester_id, target_id),
    CONSTRAINT ck_view_status CHECK (status IN ('pending','approved','denied','revoked'))
);
