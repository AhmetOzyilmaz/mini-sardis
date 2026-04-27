CREATE TABLE offers (
    id              UUID          NOT NULL,
    name            VARCHAR(100)  NOT NULL,
    description     VARCHAR(500),
    plan_id         UUID          NOT NULL,
    promo_code_id   UUID,
    target_type     VARCHAR(30)   NOT NULL DEFAULT 'ALL_USERS',
    target_user_id  UUID,
    target_plan_id  UUID,
    valid_from      TIMESTAMP,
    valid_to        TIMESTAMP,
    active          BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP     NOT NULL,
    CONSTRAINT pk_offers        PRIMARY KEY (id),
    CONSTRAINT fk_offers_plan   FOREIGN KEY (plan_id) REFERENCES subscription_plans(id),
    CONSTRAINT fk_offers_promo  FOREIGN KEY (promo_code_id) REFERENCES promo_codes(id),
    CONSTRAINT ck_target_type   CHECK (target_type IN ('ALL_USERS','SPECIFIC_USER','PLAN_UPGRADE'))
);

CREATE INDEX idx_offers_active      ON offers(active);
CREATE INDEX idx_offers_target_user ON offers(target_user_id);
