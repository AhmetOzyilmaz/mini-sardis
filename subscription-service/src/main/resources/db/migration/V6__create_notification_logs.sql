CREATE TABLE notification_logs (
    id         UUID        NOT NULL,
    user_id    UUID        NOT NULL,
    channel    VARCHAR(20) NOT NULL,
    subject    VARCHAR(255),
    body       TEXT        NOT NULL,
    sent_at    TIMESTAMP   NOT NULL,
    success    BOOLEAN     NOT NULL,
    CONSTRAINT pk_notification_logs       PRIMARY KEY (id),
    CONSTRAINT fk_notification_logs_user  FOREIGN KEY (user_id) REFERENCES users(id)
);
