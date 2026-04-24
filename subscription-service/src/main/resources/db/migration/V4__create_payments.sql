CREATE TABLE payments (
    id              UUID          NOT NULL,
    subscription_id UUID          NOT NULL,
    idempotency_key VARCHAR(255)  NOT NULL,
    amount          DECIMAL(10,2) NOT NULL,
    currency        VARCHAR(3)    NOT NULL DEFAULT 'TRY',
    status          VARCHAR(20)   NOT NULL DEFAULT 'PENDING',
    type            VARCHAR(20)   NOT NULL,
    external_ref    VARCHAR(255),
    failure_reason  VARCHAR(500),
    retry_count     INT           NOT NULL DEFAULT 0,
    processed_at    TIMESTAMP,
    created_at      TIMESTAMP     NOT NULL,
    CONSTRAINT pk_payments                PRIMARY KEY (id),
    CONSTRAINT uq_payments_idempotency    UNIQUE (idempotency_key),
    CONSTRAINT fk_payments_subscription   FOREIGN KEY (subscription_id) REFERENCES subscriptions(id)
);
