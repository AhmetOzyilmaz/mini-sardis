CREATE TABLE promo_codes (
    id             UUID          NOT NULL,
    code           VARCHAR(20)   NOT NULL,
    discount_type  VARCHAR(20)   NOT NULL,
    discount_value DECIMAL(10,2) NOT NULL,
    max_uses       INT,
    current_uses   INT           NOT NULL DEFAULT 0,
    active         BOOLEAN       NOT NULL DEFAULT TRUE,
    valid_from     TIMESTAMP,
    valid_to       TIMESTAMP,
    created_at     TIMESTAMP     NOT NULL,
    CONSTRAINT pk_promo_codes  PRIMARY KEY (id),
    CONSTRAINT uq_promo_code   UNIQUE (code),
    CONSTRAINT ck_discount_type CHECK (discount_type IN ('PERCENTAGE','FIXED_AMOUNT')),
    CONSTRAINT ck_discount_value CHECK (discount_value > 0)
);

CREATE INDEX idx_promo_codes_code ON promo_codes(code);
