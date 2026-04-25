ALTER TABLE payments ADD COLUMN IF NOT EXISTS payment_method VARCHAR(20) DEFAULT 'CREDIT_CARD' NOT NULL;
CREATE INDEX IF NOT EXISTS idx_payments_payment_method ON payments (payment_method);
