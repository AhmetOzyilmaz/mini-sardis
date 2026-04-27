CREATE TABLE refunds (
    id              UUID          NOT NULL,
    payment_id      UUID          NOT NULL,
    subscription_id UUID          NOT NULL,
    user_id         UUID          NOT NULL,
    amount          DECIMAL(10,2) NOT NULL,
    currency        VARCHAR(3)    NOT NULL DEFAULT 'TRY',
    status          VARCHAR(20)   NOT NULL DEFAULT 'REQUESTED',
    reason          VARCHAR(500),
    requested_at    TIMESTAMP     NOT NULL,
    processed_at    TIMESTAMP,
    CONSTRAINT pk_refunds         PRIMARY KEY (id),
    CONSTRAINT fk_refunds_payment FOREIGN KEY (payment_id) REFERENCES payments(id),
    CONSTRAINT ck_refund_status   CHECK (status IN ('REQUESTED','PROCESSING','COMPLETED','REJECTED'))
);

CREATE INDEX idx_refunds_subscription_id ON refunds(subscription_id);
CREATE INDEX idx_refunds_status          ON refunds(status);
