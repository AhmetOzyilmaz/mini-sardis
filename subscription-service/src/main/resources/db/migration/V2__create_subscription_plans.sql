CREATE TABLE subscription_plans (
    id            UUID          NOT NULL,
    name          VARCHAR(100)  NOT NULL,
    description   TEXT,
    price         DECIMAL(10,2) NOT NULL,
    duration_days INT           NOT NULL DEFAULT 30,
    trial_days    INT           NOT NULL DEFAULT 0,
    active        BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMP     NOT NULL,
    CONSTRAINT pk_subscription_plans      PRIMARY KEY (id),
    CONSTRAINT uq_subscription_plans_name UNIQUE (name)
);
