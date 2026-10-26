package com.entrevio.entrevio_backend.model;

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
@Table(
        name = "matches",
        uniqueConstraints = @UniqueConstraint(columnNames = {"job_id", "candidate_id"})
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Match {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id", nullable = false)
    @ToString.Exclude
    @JsonIgnore
    private Job job;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "candidate_id", nullable = false)
    @ToString.Exclude
    @JsonIgnore
    private User candidate;

    /** 0-100 overall score */
    @Column(nullable = false)
    private Integer score;

    /** JSON: { skills, experience, education, seniority } each 0-100 */
    @Column(name = "breakdown_json", columnDefinition = "TEXT")
    private String breakdownJson;

    /** Plain-English explanation (LLM or template) */
    @Column(columnDefinition = "TEXT")
    private String explanation;

    /** true if explanation came from the LLM, false if templated */
    @Column(name = "llm_generated", nullable = false)
    private boolean llmGenerated;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (id == null) id = UUID.randomUUID();
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}