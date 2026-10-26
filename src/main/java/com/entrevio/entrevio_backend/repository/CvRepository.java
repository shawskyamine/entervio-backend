package com.entrevio.entrevio_backend.repository;

import com.entrevio.entrevio_backend.model.CvDocument;
import com.entrevio.entrevio_backend.model.enums.CvStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CvRepository extends JpaRepository<CvDocument, UUID> {

    Optional<CvDocument> findByUserId(UUID userId);

    boolean existsByUserId(UUID userId);

    List<CvDocument> findByStatus(CvStatus status);
}