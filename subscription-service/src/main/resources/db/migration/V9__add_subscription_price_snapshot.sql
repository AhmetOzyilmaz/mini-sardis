-- Stores the price snapshot at time of subscription creation so the Payment
-- Service knows what to charge without querying the plan catalog.
ALTER TABLE subscriptions
    ADD COLUMN IF NOT EXISTS amount   DECIMAL(10,2),
    ADD COLUMN IF NOT EXISTS currency VARCHAR(3) DEFAULT 'TRY';

-- Add currency column to subscription_plans (the seed data uses TRY implicitly)
ALTER TABLE subscription_plans
    ADD COLUMN IF NOT EXISTS currency VARCHAR(3) NOT NULL DEFAULT 'TRY';
