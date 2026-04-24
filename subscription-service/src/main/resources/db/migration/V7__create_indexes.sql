-- Subscription lookups by user (most common query pattern)
CREATE INDEX idx_subscriptions_user_id ON subscriptions(user_id);

-- Active subscription status filter (admin dashboard, monitoring)
CREATE INDEX idx_subscriptions_status ON subscriptions(status);

-- Renewal scheduler: find active subscriptions due for renewal
-- Note: In PostgreSQL production, replace with partial index:
--   CREATE INDEX idx_subscriptions_renewal ON subscriptions(next_renewal_date) WHERE status = 'ACTIVE';
CREATE INDEX idx_subscriptions_status_renewal ON subscriptions(status, next_renewal_date);

-- Payment lookups by subscription (history queries)
CREATE INDEX idx_payments_subscription_id ON payments(subscription_id);

-- Outbox poller: unprocessed events ordered by creation time
-- Note: In PostgreSQL production, replace with partial index:
--   CREATE INDEX idx_outbox_unprocessed ON outbox_events(created_at) WHERE processed = false;
CREATE INDEX idx_outbox_events_processed_created ON outbox_events(processed, created_at);

-- Notification history by user
CREATE INDEX idx_notification_logs_user_id ON notification_logs(user_id);

-- Plan lookups by active flag (cache eviction is rare; this covers uncached reads)
CREATE INDEX idx_plans_active ON subscription_plans(active);

-- User email lookup — JWT login path (hot path: every login + token validation)
CREATE INDEX idx_users_email ON users(email);
