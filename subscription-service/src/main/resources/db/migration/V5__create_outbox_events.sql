CREATE TABLE outbox_events (
    id             UUID         NOT NULL,
    aggregate_id   UUID         NOT NULL,
    aggregate_type VARCHAR(100) NOT NULL,
    event_type     VARCHAR(100) NOT NULL,
    payload        TEXT         NOT NULL,
    processed      BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at     TIMESTAMP    NOT NULL,
    processed_at   TIMESTAMP,
    CONSTRAINT pk_outbox_events PRIMARY KEY (id)
);
