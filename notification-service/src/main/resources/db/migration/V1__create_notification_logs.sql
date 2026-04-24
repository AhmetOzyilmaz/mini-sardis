CREATE TABLE notification_logs (
    id          UUID            NOT NULL,
    user_id     UUID            NOT NULL,
    channel     VARCHAR(20)     NOT NULL,
    subject     VARCHAR(500)    NOT NULL,
    body        TEXT            NOT NULL,
    success     BOOLEAN         NOT NULL DEFAULT TRUE,
    sent_at     TIMESTAMP       NOT NULL,
    CONSTRAINT pk_notification_logs PRIMARY KEY (id)
);

CREATE INDEX idx_notification_logs_user_id ON notification_logs (user_id);
CREATE INDEX idx_notification_logs_sent_at  ON notification_logs (sent_at DESC);
