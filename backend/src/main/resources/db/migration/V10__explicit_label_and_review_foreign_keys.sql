-- Explicit named foreign keys for databases which ignore inline REFERENCES.
-- Existing migration files remain unchanged. H2 may already have inline keys;
-- these compatible named keys also document and enforce the MySQL contract.
ALTER TABLE dispatch_tasks ADD CONSTRAINT fk_dispatch_task_label FOREIGN KEY (label_id) REFERENCES work_labels(id);
ALTER TABLE work_logs ADD CONSTRAINT fk_work_log_label FOREIGN KEY (label_id) REFERENCES work_labels(id);
ALTER TABLE task_reviews ADD CONSTRAINT fk_task_review_task FOREIGN KEY (task_id) REFERENCES dispatch_tasks(id);
ALTER TABLE task_reviews ADD CONSTRAINT fk_task_review_applicant FOREIGN KEY (applicant_id) REFERENCES users(id);
ALTER TABLE task_reviews ADD CONSTRAINT fk_task_review_reviewer FOREIGN KEY (reviewer_id) REFERENCES users(id);
ALTER TABLE notifications ADD CONSTRAINT fk_notification_recipient FOREIGN KEY (recipient_id) REFERENCES users(id);
