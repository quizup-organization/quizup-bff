-- V2: Abonnements Web Push (une ligne par navigateur/appareil abonné).
-- La PK est l'endpoint du service de push : un endpoint appartient à un seul compte à la fois
-- (le rebind au login écrase user_id). Les clés p256dh/auth sont celles de l'abonnement navigateur.

CREATE TABLE push_subscription (
    endpoint   VARCHAR(1024) NOT NULL,
    user_id    VARCHAR(64)   NOT NULL,
    p256dh     VARCHAR(255)  NOT NULL,
    auth       VARCHAR(255)  NOT NULL,
    user_agent VARCHAR(255),
    created_at TIMESTAMP     NOT NULL,
    updated_at TIMESTAMP     NOT NULL,
    PRIMARY KEY (endpoint)
);

CREATE INDEX idx_push_subscription_user ON push_subscription (user_id);
