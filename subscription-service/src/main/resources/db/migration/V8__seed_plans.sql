INSERT INTO subscription_plans (id, name, description, price, duration_days, trial_days, active, created_at) VALUES
    ('a0000001-0000-0000-0000-000000000001', 'Basic',      'Temel abonelik paketi — sınırlı özellikler', 49.99,  30, 7, TRUE, CURRENT_TIMESTAMP),
    ('a0000001-0000-0000-0000-000000000002', 'Pro',        'Profesyonel abonelik paketi — tam özellik seti', 99.99,  30, 7, TRUE, CURRENT_TIMESTAMP),
    ('a0000001-0000-0000-0000-000000000003', 'Enterprise', 'Kurumsal abonelik paketi — öncelikli destek', 249.99, 30, 0, TRUE, CURRENT_TIMESTAMP);
