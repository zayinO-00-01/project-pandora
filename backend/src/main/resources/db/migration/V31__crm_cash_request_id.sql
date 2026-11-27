ALTER TABLE crm_cash ADD COLUMN request_id VARCHAR(36) NULL;
CREATE UNIQUE INDEX uq_crm_cash_actor_request ON crm_cash (actor_id, request_id);
