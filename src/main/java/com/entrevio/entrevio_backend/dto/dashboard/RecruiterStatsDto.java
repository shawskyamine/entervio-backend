package com.entrevio.entrevio_backend.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecruiterStatsDto {
    private long totalApplications;
    private long activeJobs;
    private long totalJobs;
    private long pendingApplications;
    private long interviewsCount;
    private long acceptedCount;
    private long rejectedCount;
    private double averageMatchScore;
    private double averageTimeToFirstReplyDays;
}