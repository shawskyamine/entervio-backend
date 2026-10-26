package com.entrevio.entrevio_backend.repository;

import com.entrevio.entrevio_backend.model.Job;
import com.entrevio.entrevio_backend.model.enums.JobStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface JobRepository extends JpaRepository<Job, UUID> {

    List<Job> findByCompanyIdOrderByCreatedAtDesc(UUID companyId);

    List<Job> findByStatusOrderByCreatedAtDesc(JobStatus status);

    long countByCompanyId(UUID companyId);

    long countByCompanyIdAndStatus(UUID companyId, JobStatus status);
}