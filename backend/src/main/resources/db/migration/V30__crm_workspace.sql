CREATE TABLE crm_write_guard (id INT PRIMARY KEY);
INSERT INTO crm_write_guard (id) VALUES (1);

CREATE TABLE crm_permissions (
  user_id BIGINT NOT NULL,
  permission_code VARCHAR(32) NOT NULL,
  PRIMARY KEY (user_id, permission_code),
  FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE crm_customers (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  kind VARCHAR(16) NOT NULL,
  name VARCHAR(120) NOT NULL,
  contact_name VARCHAR(120) NOT NULL,
  phone VARCHAR(30) NOT NULL,
  name_key VARCHAR(120) NOT NULL,
  phone_key VARCHAR(30) NOT NULL,
  created_by BIGINT NOT NULL,
  created_at TIMESTAMP NOT NULL,
  FOREIGN KEY (created_by) REFERENCES users(id)
);
CREATE INDEX idx_crm_customer_name ON crm_customers (name_key);
CREATE INDEX idx_crm_customer_phone ON crm_customers (phone_key);

CREATE TABLE crm_customer_links (
  customer_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  linked_by BIGINT NOT NULL,
  linked_at TIMESTAMP NOT NULL,
  PRIMARY KEY (customer_id, user_id),
  FOREIGN KEY (customer_id) REFERENCES crm_customers(id),
  FOREIGN KEY (user_id) REFERENCES users(id),
  FOREIGN KEY (linked_by) REFERENCES users(id)
);

CREATE TABLE crm_services (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  customer_id BIGINT NOT NULL,
  owner_id BIGINT NOT NULL,
  kind VARCHAR(24) NOT NULL,
  status VARCHAR(16) NOT NULL,
  scheduled_at DATETIME NOT NULL,
  actual_at DATETIME,
  description VARCHAR(2000) NOT NULL,
  created_at TIMESTAMP NOT NULL,
  receivable_cents BIGINT NOT NULL DEFAULT 0,
  FOREIGN KEY (customer_id) REFERENCES crm_customers(id),
  FOREIGN KEY (owner_id) REFERENCES users(id)
);
CREATE INDEX idx_crm_service_customer ON crm_services (customer_id);
CREATE INDEX idx_crm_service_owner ON crm_services (owner_id);

CREATE TABLE crm_service_members (
  service_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  PRIMARY KEY (service_id, user_id),
  FOREIGN KEY (service_id) REFERENCES crm_services(id),
  FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE crm_service_tasks (
  service_id BIGINT NOT NULL,
  task_id BIGINT NOT NULL,
  PRIMARY KEY (service_id, task_id),
  FOREIGN KEY (service_id) REFERENCES crm_services(id),
  FOREIGN KEY (task_id) REFERENCES dispatch_tasks(id) ON DELETE CASCADE
);

CREATE TABLE crm_service_events (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  service_id BIGINT NOT NULL,
  actor_id BIGINT NOT NULL,
  event_type VARCHAR(24) NOT NULL,
  detail VARCHAR(3000) NOT NULL,
  recorded_at TIMESTAMP NOT NULL,
  FOREIGN KEY (service_id) REFERENCES crm_services(id),
  FOREIGN KEY (actor_id) REFERENCES users(id)
);

CREATE TABLE crm_shop_members (
  source_code VARCHAR(40) NOT NULL,
  member_number VARCHAR(80) NOT NULL,
  customer_id BIGINT NOT NULL,
  PRIMARY KEY (source_code, member_number),
  FOREIGN KEY (customer_id) REFERENCES crm_customers(id)
);

CREATE TABLE crm_orders (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  source_code VARCHAR(40) NOT NULL,
  member_number VARCHAR(80) NOT NULL,
  external_number VARCHAR(80) NOT NULL,
  customer_id BIGINT NOT NULL,
  owner_id BIGINT NOT NULL,
  product VARCHAR(24) NOT NULL,
  total_cents BIGINT NOT NULL,
  paid_on DATE,
  refunded_cents BIGINT NOT NULL,
  last_refund_on DATE,
  UNIQUE (source_code, external_number),
  FOREIGN KEY (customer_id) REFERENCES crm_customers(id),
  FOREIGN KEY (owner_id) REFERENCES users(id),
  FOREIGN KEY (source_code,member_number) REFERENCES crm_shop_members(source_code,member_number)
);
CREATE INDEX idx_crm_order_customer ON crm_orders (customer_id);

CREATE TABLE crm_card_uses (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  order_id BIGINT NOT NULL,
  service_id BIGINT NOT NULL,
  used_by BIGINT NOT NULL,
  used_at TIMESTAMP NOT NULL,
  reversed_by BIGINT,
  reversed_at TIMESTAMP,
  reverse_reason VARCHAR(500),
  UNIQUE (service_id),
  FOREIGN KEY (order_id) REFERENCES crm_orders(id),
  FOREIGN KEY (service_id) REFERENCES crm_services(id),
  FOREIGN KEY (used_by) REFERENCES users(id),
  FOREIGN KEY (reversed_by) REFERENCES users(id)
);

CREATE TABLE crm_cash (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  service_id BIGINT,
  order_id BIGINT,
  direction VARCHAR(8) NOT NULL,
  amount_cents BIGINT NOT NULL,
  booked_on DATE NOT NULL,
  actor_id BIGINT NOT NULL,
  note VARCHAR(500) NOT NULL,
  FOREIGN KEY (service_id) REFERENCES crm_services(id),
  FOREIGN KEY (order_id) REFERENCES crm_orders(id),
  FOREIGN KEY (actor_id) REFERENCES users(id),
  CHECK ((service_id IS NULL) <> (order_id IS NULL)),
  CHECK (direction IN ('RECEIPT','REFUND') AND amount_cents>0)
);
CREATE INDEX idx_crm_cash_service ON crm_cash (service_id);
CREATE INDEX idx_crm_cash_month ON crm_cash (booked_on);

CREATE TABLE crm_staff_profiles (
  user_id BIGINT PRIMARY KEY,
  department VARCHAR(64) NOT NULL,
  job_title VARCHAR(64) NOT NULL,
  FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE crm_contracts (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  title VARCHAR(120) NOT NULL,
  start_on DATE NOT NULL,
  end_on DATE NOT NULL,
  file_name VARCHAR(180) NOT NULL,
  pdf_data MEDIUMBLOB NOT NULL,
  recorded_by BIGINT NOT NULL,
  FOREIGN KEY (user_id) REFERENCES users(id),
  FOREIGN KEY (recorded_by) REFERENCES users(id)
);

CREATE TABLE crm_contract_reminders (
  contract_id BIGINT NOT NULL,
  end_on DATE NOT NULL,
  recipient_id BIGINT NOT NULL,
  PRIMARY KEY (contract_id, end_on, recipient_id),
  FOREIGN KEY (contract_id) REFERENCES crm_contracts(id),
  FOREIGN KEY (recipient_id) REFERENCES users(id)
);

CREATE TABLE crm_audit (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  actor_id BIGINT NOT NULL,
  event_type VARCHAR(32) NOT NULL,
  detail VARCHAR(1000) NOT NULL,
  recorded_at TIMESTAMP NOT NULL,
  FOREIGN KEY (actor_id) REFERENCES users(id)
);
