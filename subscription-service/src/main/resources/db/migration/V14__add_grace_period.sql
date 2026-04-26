ALTER TABLE subscriptions ADD COLUMN IF NOT EXISTS grace_period_end_date DATE;

-- Compound index for scheduler query (H2-compatible; PostgreSQL partial index preferred in production)
CREATE INDEX idx_subscriptions_grace_period ON subscriptions(status, grace_period_end_date);
