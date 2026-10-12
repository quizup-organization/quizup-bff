package io.github.quizup.bff.infrastructure.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface MediaAssetJpaRepository extends JpaRepository<MediaAssetEntity, UUID> {

    Optional<MediaAssetEntity> findBySha256AndUploadedBy(String sha256, String uploadedBy);

    long countByUploadedBy(String uploadedBy);

    @Query("select coalesce(sum(asset.sizeBytes), 0) from MediaAssetEntity asset where asset.uploadedBy = :uploadedBy")
    long sumBytesByUploadedBy(@Param("uploadedBy") String uploadedBy);

    long countByObjectKey(String objectKey);
}
