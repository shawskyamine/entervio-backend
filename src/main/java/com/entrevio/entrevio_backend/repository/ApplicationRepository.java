package com.entrevio.entrevio_backend.repository;

import com.entrevio.entrevio_backend.model.Application;
import com.entrevio.entrevio_backend.model.enums.ApplicationStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ApplicationRepository extends JpaRepository<Application, UUID> {

    // ─── Existing methods (Phase 6) ────────────────────────────

    Optional<Application> findByCandidateIdAndJobId(UUID candidateId, UUID jobId);

    List<Application> findByCandidateIdOrderByCreatedAtDesc(UUID candidateId);

    List<Application> findByJobIdOrderByCreatedAtDesc(UUID jobId);

    long countByJobId(UUID jobId);

    long countByCandidateId(UUID candidateId);

    long countByCandidateIdAndStatus(UUID candidateId, ApplicationStatus status);

    long countByJobIdAndStatus(UUID jobId, ApplicationStatus status);

    boolean existsByCandidateIdAndJobId(UUID candidateId, UUID jobId);

    // ─── Analytics aggregations (Phase 9) ──────────────────────

    /**
     * Total number of applications received by a recruiter,
     * across all their jobs.
     */
    @Query("""
        SELECT COUNT(a) FROM Application a
        WHERE a.job.company.owner.id = :ownerId
        """)
    long countByOwnerId(@Param("ownerId") UUID ownerId);

    /**
     * Count of applications in a specific status, for a recruiter.
     */
    @Query("""
        SELECT COUNT(a) FROM Application a
        WHERE a.job.company.owner.id = :ownerId
        AND a.status = :status
        """)
    long countByOwnerIdAndStatus(
            @Param("ownerId") UUID ownerId,
            @Param("status") ApplicationStatus status
    );

    /**
     * Group all applications by status, for a recruiter.
     * Returns [ApplicationStatus, Long]
     */
    @Query("""
        SELECT a.status, COUNT(a)
        FROM Application a
        WHERE a.job.company.owner.id = :ownerId
        GROUP BY a.status
        """)
    List<Object[]> groupByStatusForOwner(@Param("ownerId") UUID ownerId);

    /**
     * Applications per day, for a recruiter, since a given date.
     * Native query because Hibernate 6's HQL FUNCTION('DATE', ...)
     * doesn't type-check correctly against LocalDateTime columns.
     * Returns [java.sql.Date, Long]
     */
    @Query(value = """
        SELECT DATE(a.created_at) AS day, COUNT(*)
        FROM applications a
        JOIN jobs j ON j.id = a.job_id
        JOIN companies c ON c.id = j.company_id
        WHERE c.owner_user_id = :ownerId
        AND a.created_at >= :since
        GROUP BY DATE(a.created_at)
        ORDER BY day ASC
        """, nativeQuery = true)
    List<Object[]> dailyApplicationsForOwner(
            @Param("ownerId") UUID ownerId,
            @Param("since") LocalDateTime since
    );

    /**
     * Top jobs by application count, for a recruiter.
     * Returns [UUID jobId, String title, JobStatus status, Long count]
     */
    @Query("""
        SELECT j.id, j.title, j.status, COUNT(a)
        FROM Application a
        JOIN a.job j
        WHERE j.company.owner.id = :ownerId
        GROUP BY j.id, j.title, j.status
        ORDER BY COUNT(a) DESC
        """)
    List<Object[]> topJobsByApplications(
            @Param("ownerId") UUID ownerId,
            Pageable pageable
    );

    /**
     * Average match score for all applications of a recruiter.
     * Uses the Match entity — but query runs against Application's
     * repository because it's a dashboard aggregation, not a Match query.
     */
    @Query("""
        SELECT AVG(m.score)
        FROM Match m
        WHERE m.job.company.owner.id = :ownerId
        """)
    Double averageMatchScoreForOwner(@Param("ownerId") UUID ownerId);

    /**
     * Average seconds between application creation and last update,
     * for applications that have moved past PENDING.
     *
     * Native query because Hibernate 6's FUNCTION('TIMESTAMPDIFF', 'SECOND', ...)
     * rejects the string unit 'SECOND' (expects a TEMPORAL_UNIT enum).
     * Postgres EXTRACT(EPOCH FROM interval) gives the interval in seconds.
     */
    @Query(value = """
        SELECT AVG(EXTRACT(EPOCH FROM (a.updated_at - a.created_at)))
        FROM applications a
        JOIN jobs j ON j.id = a.job_id
        JOIN companies c ON c.id = j.company_id
        WHERE c.owner_user_id = :ownerId
        AND a.status <> 'PENDING'
        """, nativeQuery = true)
    Double averageResponseSecondsForOwner(@Param("ownerId") UUID ownerId);
}