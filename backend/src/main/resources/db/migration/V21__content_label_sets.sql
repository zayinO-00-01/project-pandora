CREATE TABLE content_label_sets (kind VARCHAR(16) NOT NULL, content_id BIGINT NOT NULL, label_ids TEXT NOT NULL, PRIMARY KEY(kind,content_id));
