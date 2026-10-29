ALTER TABLE work_logs ADD COLUMN end_date DATE;
UPDATE work_logs SET end_date = log_date WHERE end_date IS NULL;
ALTER TABLE work_logs ADD CONSTRAINT ck_log_date_range CHECK (end_date IS NULL OR end_date >= log_date);
