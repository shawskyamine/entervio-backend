package com.entrevio.entrevio_backend.model;

import com.entrevio.entrevio_backend.model.enums.ChunkKind;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "cv_chunks")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CvChunk {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cv_id", nullable = false)
    @ToString.Exclude
    @JsonIgnore
    private CvDocument cv;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "candidate_id", nullable = false)
    @ToString.Exclude
    @JsonIgnore
    private User candidate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ChunkKind kind;

    @Column(name = "chunk_index", nullable = false)
    private Integer chunkIndex;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(name = "token_count", nullable = false)
    private Integer tokenCount;

    /*
     * The `embedding` column (pgvector vector(768)) is intentionally NOT mapped
     * as a JPA field. Hibernate cannot bind the pgvector type directly, and we
     * don't want Hibernate's ddl-auto trying to create or alter the column.
     *
     * Flyway owns the schema (see V1__cv_chunks.sql).
     * Reads and writes go through native SQL in CvChunkRepository.
     */

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (id == null) id = UUID.randomUUID();
        createdAt = LocalDateTime.now();
    }
}