-- V3: Catalogue des images uploadées via le BFF (stockage objet MinIO `quizup-media`).
-- Une ligne = une référence utilisateur à un objet immuable ; la clé d'objet est dérivée du
-- sha256 (déduplication), plusieurs utilisateurs peuvent référencer le même objet.

CREATE TABLE media_asset (
    id           UUID         NOT NULL,
    object_key   VARCHAR(512) NOT NULL,
    url          VARCHAR(1024) NOT NULL,
    content_type VARCHAR(64)  NOT NULL,
    size_bytes   BIGINT       NOT NULL,
    width        INTEGER      NOT NULL,
    height       INTEGER      NOT NULL,
    sha256       VARCHAR(64)  NOT NULL,
    uploaded_by  VARCHAR(64)  NOT NULL,
    created_at   TIMESTAMP    NOT NULL,
    PRIMARY KEY (id)
);

CREATE INDEX idx_media_asset_uploaded_by ON media_asset (uploaded_by);
CREATE INDEX idx_media_asset_sha256 ON media_asset (sha256);
CREATE INDEX idx_media_asset_object_key ON media_asset (object_key);
