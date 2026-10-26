package com.entrevio.entrevio_backend.repository;

import com.entrevio.entrevio_backend.model.Match;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MatchRepository extends JpaRepository<Match, UUID> {

    List<Match> findByJobIdOrderByScoreDesc(UUID jobId);

    Optional<Match> findByJobIdAndCandidateId(UUID jobId, UUID candidateId);

    void deleteByJobId(UUID jobId);

    long countByJobId(UUID jobId);
}