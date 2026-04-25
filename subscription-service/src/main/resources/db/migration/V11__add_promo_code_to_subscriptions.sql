ALTER TABLE subscriptions ADD COLUMN IF NOT EXISTS promo_code_id UUID DEFAULT NULL;
ALTER TABLE subscriptions ADD COLUMN IF NOT EXISTS discount_amount DECIMAL(10,2) NOT NULL DEFAULT 0.00;
ALTER TABLE subscriptions ADD COLUMN IF NOT EXISTS final_amount DECIMAL(10,2);

UPDATE subscriptions SET final_amount = amount WHERE final_amount IS NULL;

ALTER TABLE subscriptions ALTER COLUMN final_amount SET NOT NULL;
