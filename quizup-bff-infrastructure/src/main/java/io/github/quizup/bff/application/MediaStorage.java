package io.github.quizup.bff.application;

/**
 * Port de stockage objet (MinIO/S3-compatible). Les objets sont immuables : même clé = même
 * contenu (clé dérivée du sha256), `Cache-Control` long.
 */
public interface MediaStorage {

    void put(String objectKey, byte[] content, String contentType, String sha256);

    void delete(String objectKey);
}
