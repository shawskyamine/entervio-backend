package com.entrevio.entrevio_backend.service;

import com.entrevio.entrevio_backend.dto.dashboard.PipelineDto;
import com.entrevio.entrevio_backend.dto.dashboard.RecruiterStatsDto;
import com.entrevio.entrevio_backend.dto.dashboard.TimelinePointDto;
import com.entrevio.entrevio_backend.dto.dashboard.TopJobDto;
import com.entrevio.entrevio_backend.model.Application;
import com.entrevio.entrevio_backend.model.Job;
import com.entrevio.entrevio_backend.model.User;
import com.entrevio.entrevio_backend.model.enums.ApplicationStatus;
import com.entrevio.entrevio_backend.model.enums.JobStatus;
import com.entrevio.entrevio_backend.model.enums.Role;
import com.entrevio.entrevio_backend.repository.ApplicationRepository;
import com.entrevio.entrevio_backend.repository.JobRepository;
import com.entrevio.entrevio_backend.security.CurrentUser;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class DashboardService {

    private final ApplicationRepository applicationRepository;
    private final JobRepository jobRepository;

    public DashboardService(ApplicationRepository applicationRepository,
                            JobRepository jobRepository) {
        this.applicationRepository = applicationRepository;
        this.jobRepository = jobRepository;
    }

    // =============== KPI Stats ===============

    @Transactional(readOnly = true)
    public RecruiterStatsDto getRecruiterStats() {
        User me = CurrentUser.requireRole(Role.RECRUITER);
        UUID ownerId = me.getId();

        long totalApplications = applicationRepository.countByOwnerId(ownerId);
        long activeJobs = jobRepository.countByCompanyIdAndStatus(me.getCompany().getId(), JobStatus.OPEN);
        long totalJobs = jobRepository.countByCompanyId(me.getCompany().getId());

        long pending = applicationRepository.countByOwnerIdAndStatus(
                ownerId, ApplicationStatus.PENDING);
        long accepted = applicationRepository.countByOwnerIdAndStatus(
                ownerId, ApplicationStatus.ACCEPTED);
        long rejected = applicationRepository.countByOwnerIdAndStatus(
                ownerId, ApplicationStatus.REJECTED);

        Double avgScore = applicationRepository.averageMatchScoreForOwner(ownerId);
        Double avgSeconds = applicationRepository.averageResponseSecondsForOwner(ownerId);

        return RecruiterStatsDto.builder()
                .totalApplications(totalApplications)
                .activeJobs(activeJobs)
                .totalJobs(totalJobs)
                .pendingApplications(pending)
                .interviewsCount(0) // placeholder — interviews not built yet
                .acceptedCount(accepted)
                .rejectedCount(rejected)
                .averageMatchScore(avgScore != null ? Math.round(avgScore * 10.0) / 10.0 : 0)
                .averageTimeToFirstReplyDays(
                        avgSeconds != null ? Math.round(avgSeconds / 864.0) / 100.0 : 0)
                .build();
    }

    // =============== Timeline (last N days) ===============

    @Transactional(readOnly = true)
    public List<TimelinePointDto> getTimeline(int days) {
        User me = CurrentUser.requireRole(Role.RECRUITER);
        LocalDateTime since = LocalDateTime.now().minusDays(days).withHour(0).withMinute(0);

        List<Object[]> rows = applicationRepository.dailyApplicationsForOwner(
                me.getId(), since);

        // Build a map of date → count, then fill gaps with 0 for a continuous line
        Map<LocalDate, Long> counts = new HashMap<>();
        for (Object[] row : rows) {
            LocalDate date = ((java.sql.Date) row[0]).toLocalDate();
            Long count = ((Number) row[1]).longValue();
            counts.put(date, count);
        }

        List<TimelinePointDto> result = new ArrayList<>();
        LocalDate today = LocalDate.now();
        for (int i = days - 1; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            result.add(TimelinePointDto.builder()
                    .date(date)
                    .applications(counts.getOrDefault(date, 0L))
                    .build());
        }
        return result;
    }

    // =============== Pipeline funnel ===============

    @Transactional(readOnly = true)
    public PipelineDto getPipeline() {
        User me = CurrentUser.requireRole(Role.RECRUITER);

        Map<ApplicationStatus, Long> counts = new EnumMap<>(ApplicationStatus.class);
        for (Object[] row : applicationRepository.groupByStatusForOwner(me.getId())) {
            counts.put((ApplicationStatus) row[0], ((Number) row[1]).longValue());
        }

        return PipelineDto.builder()
                .applied(counts.getOrDefault(ApplicationStatus.PENDING, 0L))
                .reviewing(counts.getOrDefault(ApplicationStatus.REVIEWING, 0L))
                .accepted(counts.getOrDefault(ApplicationStatus.ACCEPTED, 0L))
                .rejected(counts.getOrDefault(ApplicationStatus.REJECTED, 0L))
                .withdrawn(counts.getOrDefault(ApplicationStatus.WITHDRAWN, 0L))
                .build();
    }

    // =============== Top jobs ===============

    @Transactional(readOnly = true)
    public List<TopJobDto> getTopJobs(int limit) {
        User me = CurrentUser.requireRole(Role.RECRUITER);

        List<Object[]> rows = applicationRepository.topJobsByApplications(
                me.getId(), PageRequest.of(0, limit));

        return rows.stream()
                .map(row -> {
                    UUID jobId = (UUID) row[0];
                    String title = (String) row[1];
                    JobStatus status = (JobStatus) row[2];
                    long count = ((Number) row[3]).longValue();

                    // Count reviewing/accepted per job for the table
                    long reviewing = applicationRepository.countByJobIdAndStatus(
                            jobId, ApplicationStatus.REVIEWING);
                    long accepted = applicationRepository.countByJobIdAndStatus(
                            jobId, ApplicationStatus.ACCEPTED);

                    return TopJobDto.builder()
                            .jobId(jobId)
                            .title(title)
                            .status(status.name().toLowerCase())
                            .applicationsCount(count)
                            .reviewingCount(reviewing)
                            .acceptedCount(accepted)
                            .build();
                })
                .toList();
    }
}