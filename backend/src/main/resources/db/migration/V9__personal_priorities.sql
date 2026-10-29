CREATE TABLE personal_priority_lists (
    owner_id BIGINT PRIMARY KEY,
    revision BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_personal_priority_list_owner FOREIGN KEY (owner_id) REFERENCES users (id)
);

CREATE TABLE personal_priorities (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    owner_id BIGINT NOT NULL,
    title VARCHAR(120) NOT NULL,
    detail VARCHAR(2000) NULL,
    sort_order INT NOT NULL,
    CONSTRAINT fk_personal_priority_owner FOREIGN KEY (owner_id) REFERENCES users (id)
);
CREATE INDEX idx_personal_priority_owner_order ON personal_priorities (owner_id, sort_order, id);
