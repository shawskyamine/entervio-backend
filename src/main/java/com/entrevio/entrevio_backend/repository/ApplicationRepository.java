package com.entrevio.entrevio_backend.repository;

import com.entrevio.entrevio_backend.model.Application;
import com.entrevio.entrevio_backend.model.enums.ApplicationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ApplicationRepository extends JpaRepository<Application, UUID> {

    Optional<Application> findByCandidateIdAndJobId(UUID candidateId, UUID jobId);

    List<Application> findByCandidateIdOrderByCreatedAtDesc(UUID candidateId);

    List<Application> findByJobIdOrderByCreatedAtDesc(UUID jobId);

    long countByJobId(UUID jobId);

    long countByCandidateId(UUID candidateId);

    long countByCandidateIdAndStatus(UUID candidateId, ApplicationStatus status);

    long countByJobIdAndStatus(UUID jobId, ApplicationStatus status);

    boolean existsByCandidateIdAndJobId(UUID candidateId, UUID jobId);
}