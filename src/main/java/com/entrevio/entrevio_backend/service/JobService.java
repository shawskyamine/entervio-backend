package com.entrevio.entrevio_backend.service;

import com.entrevio.entrevio_backend.ai.JobAgent;
import com.entrevio.entrevio_backend.dto.job.CreateJobRequest;
import com.entrevio.entrevio_backend.dto.job.JobDto;
import com.entrevio.entrevio_backend.dto.job.ParsedJob;
import com.entrevio.entrevio_backend.exception.NotFoundException;
import com.entrevio.entrevio_backend.exception.ValidationException;
import com.entrevio.entrevio_backend.model.Company;
import com.entrevio.entrevio_backend.model.Job;
import com.entrevio.entrevio_backend.model.User;
import com.entrevio.entrevio_backend.model.enums.EmploymentType;
import com.entrevio.entrevio_backend.model.enums.JobStatus;
import com.entrevio.entrevio_backend.model.enums.Role;
import com.entrevio.entrevio_backend.repository.CompanyRepository;
import com.entrevio.entrevio_backend.repository.JobRepository;
import com.entrevio.entrevio_backend.security.CurrentUser;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class JobService {

    private static final Logger log = LoggerFactory.getLogger(JobService.class);

    private final JobRepository jobRepository;
    private final CompanyRepository companyRepository;
    private final JobAgent jobAgent;
    private final ObjectMapper objectMapper;

    public JobService(JobRepository jobRepository,
                      CompanyRepository companyRepository,
                      JobAgent jobAgent,
                      ObjectMapper objectMapper) {
        this.jobRepository = jobRepository;
        this.companyRepository = companyRepository;
        this.jobAgent = jobAgent;
        this.objectMapper = objectMapper;
    }

    // =============== Recruiter operations ===============

    @Transactional
    public JobDto create(CreateJobRequest req) {
        User me = CurrentUser.requireRole(Role.RECRUITER);

        Company company = companyRepository.findByOwnerId(me.getId())
                .orElseThrow(() -> new ValidationException(
                        "You must set up your company profile before posting jobs"));

        EmploymentType type = parseEmploymentType(req.getEmploymentType());

        // 1. Call the LLM to extract structured fields
        ParsedJob parsed = null;
        String parsedJson = null;
        try {
            parsed = jobAgent.parse(req.getTitle(), req.getDescription());
            parsedJson = objectMapper.writeValueAsString(parsed);
        } catch (Exception e) {
            log.warn("JobAgent failed for title '{}': {}", req.getTitle(), e.getMessage());
            // Non-fatal: job is still created with raw description, parsed is null
        }

        Job job = Job.builder()
                .company(company)
                .title(req.getTitle())
                .description(req.getDescription())
                .location(req.getLocation())
                .employmentType(type)
                .status(JobStatus.OPEN)
                .parsedJson(parsedJson)
                .build();

        jobRepository.save(job);
        return JobDto.from(job, parsed);
    }

    @Transactional
    public JobDto updateStatus(UUID jobId, String newStatus) {
        User me = CurrentUser.requireRole(Role.RECRUITER);

        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new NotFoundException("Job not found"));

        if (!job.getCompany().getOwner().getId().equals(me.getId())) {
            throw new ValidationException("You do not own this job");
        }

        JobStatus status;
        try {
            status = JobStatus.valueOf(newStatus.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ValidationException("Invalid status: " + newStatus);
        }

        job.setStatus(status);
        jobRepository.save(job);
        return toDto(job);
    }

    @Transactional
    public void delete(UUID jobId) {
        User me = CurrentUser.requireRole(Role.RECRUITER);

        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new NotFoundException("Job not found"));

        if (!job.getCompany().getOwner().getId().equals(me.getId())) {
            throw new ValidationException("You do not own this job");
        }

        jobRepository.delete(job);
    }

    // =============== Shared reads ===============

    /** Public browse — only OPEN jobs. */
    @Transactional(readOnly = true)
    public List<JobDto> listOpen() {
        return jobRepository.findByStatusOrderByCreatedAtDesc(JobStatus.OPEN)
                .stream()
                .map(this::toDto)
                .toList();
    }

    /** Recruiter view — only my company's jobs, any status. */
    @Transactional(readOnly = true)
    public List<JobDto> listMine() {
        User me = CurrentUser.requireRole(Role.RECRUITER);
        Company company = companyRepository.findByOwnerId(me.getId())
                .orElseThrow(() -> new ValidationException("No company profile"));
        return jobRepository.findByCompanyIdOrderByCreatedAtDesc(company.getId())
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public JobDto getById(UUID jobId) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new NotFoundException("Job not found"));
        return toDto(job);
    }

    // =============== Helpers ===============

    private EmploymentType parseEmploymentType(String raw) {
        if (raw == null || raw.isBlank()) return EmploymentType.FULL_TIME;
        try {
            return EmploymentType.valueOf(raw.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ValidationException("Invalid employment type: " + raw);
        }
    }

    private JobDto toDto(Job job) {
        ParsedJob parsed = null;
        if (job.getParsedJson() != null) {
            try {
                parsed = objectMapper.readValue(job.getParsedJson(), ParsedJob.class);
            } catch (Exception e) {
                log.warn("Could not deserialize job parsedJson: {}", e.getMessage());
            }
        }
        return JobDto.from(job, parsed);
    }
}