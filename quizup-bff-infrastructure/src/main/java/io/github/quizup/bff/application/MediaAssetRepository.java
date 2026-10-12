package io.github.quizup.bff.application;

import java.util.Optional;
import java.util.UUID;

/** Port de persistance des images uploadées (table {@code media_asset}, BFF). */
public interface MediaAssetRepository {

    MediaAsset save(MediaAsset asset);

    Optional<MediaAsset> findById(UUID id);

    /** Déduplication : un même contenu (sha256) n'est stocké qu'une fois par utilisateur. */
    Optional<MediaAsset> findBySha256AndUploadedBy(String sha256, String uploadedBy);

    long countByUploadedBy(String uploadedBy);

    long sumBytesByUploadedBy(String uploadedBy);

    long countByObjectKey(String objectKey);

    void delete(MediaAsset asset);
}
