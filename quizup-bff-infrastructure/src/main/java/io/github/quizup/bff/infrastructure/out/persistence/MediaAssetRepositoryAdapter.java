package io.github.quizup.bff.infrastructure.out.persistence;

import io.github.quizup.bff.application.MediaAsset;
import io.github.quizup.bff.application.MediaAssetRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

/** Adaptateur JPA du catalogue média (URLs immuables, quota et déduplication par sha256). */
@Component
public class MediaAssetRepositoryAdapter implements MediaAssetRepository {

    private final MediaAssetJpaRepository repository;

    public MediaAssetRepositoryAdapter(MediaAssetJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public MediaAsset save(MediaAsset asset) {
        repository.save(toEntity(asset));
        return asset;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<MediaAsset> findById(UUID id) {
        return repository.findById(id).map(MediaAssetRepositoryAdapter::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<MediaAsset> findBySha256AndUploadedBy(String sha256, String uploadedBy) {
        return repository.findBySha256AndUploadedBy(sha256, uploadedBy)
                .map(MediaAssetRepositoryAdapter::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public long countByUploadedBy(String uploadedBy) {
        return repository.countByUploadedBy(uploadedBy);
    }

    @Override
    @Transactional(readOnly = true)
    public long sumBytesByUploadedBy(String uploadedBy) {
        return repository.sumBytesByUploadedBy(uploadedBy);
    }

    @Override
    @Transactional(readOnly = true)
    public long countByObjectKey(String objectKey) {
        return repository.countByObjectKey(objectKey);
    }

    @Override
    @Transactional
    public void delete(MediaAsset asset) {
        repository.deleteById(asset.id());
    }

    private static MediaAssetEntity toEntity(MediaAsset asset) {
        return new MediaAssetEntity(asset.id(), asset.objectKey(), asset.url(), asset.contentType(),
                asset.sizeBytes(), asset.width(), asset.height(), asset.sha256(), asset.uploadedBy(),
                asset.createdAt());
    }

    private static MediaAsset toDomain(MediaAssetEntity entity) {
        return new MediaAsset(entity.getId(), entity.getObjectKey(), entity.getUrl(),
                entity.getContentType(), entity.getSizeBytes(), entity.getWidth(), entity.getHeight(),
                entity.getSha256(), entity.getUploadedBy(), entity.getCreatedAt());
    }
}
