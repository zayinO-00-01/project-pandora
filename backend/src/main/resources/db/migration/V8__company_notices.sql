CREATE TABLE company_notices (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(128) NOT NULL,
    content TEXT NOT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    publisher_id BIGINT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE company_notice_notes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    notice_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    content VARCHAR(2000) NOT NULL,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_company_notice_notes UNIQUE (notice_id, user_id),
    CONSTRAINT fk_company_notice_notes_notice FOREIGN KEY (notice_id) REFERENCES company_notices (id),
    CONSTRAINT fk_company_notice_notes_user FOREIGN KEY (user_id) REFERENCES users (id)
);
