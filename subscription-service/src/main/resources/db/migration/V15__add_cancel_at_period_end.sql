ALTER TABLE subscriptions ADD COLUMN IF NOT EXISTS cancel_at_period_end BOOLEAN NOT NULL DEFAULT FALSE;

CREATE INDEX idx_subscriptions_cancel_at_period_end ON subscriptions(cancel_at_period_end, next_renewal_date);
