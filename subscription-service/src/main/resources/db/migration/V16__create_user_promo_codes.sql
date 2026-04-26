CREATE TABLE user_promo_codes (
    id             UUID        NOT NULL,
    user_id        UUID        NOT NULL,
    promo_code_id  UUID        NOT NULL,
    code           VARCHAR(20) NOT NULL,
    assigned_at    TIMESTAMP   NOT NULL,
    used           BOOLEAN     NOT NULL DEFAULT FALSE,
    used_at        TIMESTAMP,
    CONSTRAINT pk_user_promo_codes  PRIMARY KEY (id),
    CONSTRAINT fk_upc_user          FOREIGN KEY (user_id)       REFERENCES users(id),
    CONSTRAINT fk_upc_promo_code    FOREIGN KEY (promo_code_id) REFERENCES promo_codes(id),
    CONSTRAINT uq_upc_user_promo    UNIQUE (user_id, promo_code_id)
);

CREATE INDEX idx_upc_user_id ON user_promo_codes(user_id);
