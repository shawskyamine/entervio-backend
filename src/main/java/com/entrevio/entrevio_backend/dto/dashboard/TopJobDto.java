package com.entrevio.entrevio_backend.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TopJobDto {
    private UUID jobId;
    private String title;
    private String status;
    private long applicationsCount;
    private long reviewingCount;
    private long acceptedCount;
}