CREATE TABLE subscriptions (
    id                  UUID        NOT NULL,
    user_id             UUID        NOT NULL,
    plan_id             UUID        NOT NULL,
    status              VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    start_date          DATE,
    end_date            DATE,
    next_renewal_date   DATE,
    cancelled_at        TIMESTAMP,
    cancellation_reason VARCHAR(500),
    version             INT         NOT NULL DEFAULT 0,
    created_at          TIMESTAMP   NOT NULL,
    updated_at          TIMESTAMP,
    CONSTRAINT pk_subscriptions       PRIMARY KEY (id),
    CONSTRAINT fk_subscriptions_user  FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_subscriptions_plan  FOREIGN KEY (plan_id) REFERENCES subscription_plans(id)
);
