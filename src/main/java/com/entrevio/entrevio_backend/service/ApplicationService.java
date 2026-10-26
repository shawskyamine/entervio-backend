package com.entrevio.entrevio_backend.service;

import com.entrevio.entrevio_backend.dto.application.ApplicationDto;
import com.entrevio.entrevio_backend.dto.application.CreateApplicationRequest;
import com.entrevio.entrevio_backend.dto.application.UpdateApplicationStatusRequest;
import com.entrevio.entrevio_backend.exception.NotFoundException;
import com.entrevio.entrevio_backend.exception.ValidationException;
import com.entrevio.entrevio_backend.model.Application;
import com.entrevio.entrevio_backend.model.CvDocument;
import com.entrevio.entrevio_backend.model.Job;
import com.entrevio.entrevio_backend.model.User;
import com.entrevio.entrevio_backend.model.enums.ApplicationStatus;
import com.entrevio.entrevio_backend.model.enums.CvStatus;
import com.entrevio.entrevio_backend.model.enums.JobStatus;
import com.entrevio.entrevio_backend.model.enums.Role;
import com.entrevio.entrevio_backend.repository.ApplicationRepository;
import com.entrevio.entrevio_backend.repository.CvRepository;
import com.entrevio.entrevio_backend.repository.JobRepository;
import com.entrevio.entrevio_backend.security.CurrentUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class ApplicationService {

    private static final Logger log = LoggerFactory.getLogger(ApplicationService.class);

    private final ApplicationRepository applicationRepository;
    private final JobRepository jobRepository;
    private final CvRepository cvRepository;

    public ApplicationService(ApplicationRepository applicationRepository,
                              JobRepository jobRepository,
                              CvRepository cvRepository) {
        this.applicationRepository = applicationRepository;
        this.jobRepository = jobRepository;
        this.cvRepository = cvRepository;
    }

    // =============== Candidate operations ===============

    @Transactional
    public ApplicationDto apply(CreateApplicationRequest req) {
        User me = CurrentUser.requireRole(Role.CANDIDATE);

        Job job = jobRepository.findById(req.getJobId())
                .orElseThrow(() -> new NotFoundException("Job not found"));

        if (job.getStatus() != JobStatus.OPEN) {
            throw new ValidationException("This job is no longer accepting applications");
        }

        // Require a parsed CV
        CvDocument cv = cvRepository.findByUserId(me.getId()).orElse(null);
        if (cv == null || cv.getStatus() != CvStatus.PARSED) {
            throw new ValidationException(
                    "You must upload and parse your CV before applying to jobs");
        }

        if (applicationRepository.existsByCandidateIdAndJobId(me.getId(), job.getId())) {
            throw new ValidationException("You have already applied to this job");
        }

        Application app = Application.builder()
                .candidate(me)
                .job(job)
                .status(ApplicationStatus.PENDING)
                .coverNote(req.getCoverNote())
                .build();

        applicationRepository.save(app);
        log.info("Candidate {} applied to job {}", me.getId(), job.getId());

        return ApplicationDto.from(app, true);
    }

    @Transactional
    public void withdraw(UUID applicationId) {
        User me = CurrentUser.requireRole(Role.CANDIDATE);

        Application app = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new NotFoundException("Application not found"));

        if (!app.getCandidate().getId().equals(me.getId())) {
            throw new ValidationException("You do not own this application");
        }

        if (app.getStatus() == ApplicationStatus.WITHDRAWN) {
            throw new ValidationException("Application is already withdrawn");
        }

        app.setStatus(ApplicationStatus.WITHDRAWN);
        applicationRepository.save(app);
        log.info("Candidate {} withdrew application {}", me.getId(), applicationId);
    }

    @Transactional(readOnly = true)
    public List<ApplicationDto> listMine() {
        User me = CurrentUser.requireRole(Role.CANDIDATE);
        boolean hasCv = cvRepository.findByUserId(me.getId())
                .map(c -> c.getStatus() == CvStatus.PARSED)
                .orElse(false);
        return applicationRepository.findByCandidateIdOrderByCreatedAtDesc(me.getId())
                .stream()
                .map(a -> ApplicationDto.from(a, hasCv))
                .toList();
    }

    // =============== Recruiter operations ===============

    @Transactional(readOnly = true)
    public List<ApplicationDto> listForJob(UUID jobId) {
        User me = CurrentUser.requireRole(Role.RECRUITER);

        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new NotFoundException("Job not found"));

        if (!job.getCompany().getOwner().getId().equals(me.getId())) {
            throw new ValidationException("You do not own this job");
        }

        return applicationRepository.findByJobIdOrderByCreatedAtDesc(jobId)
                .stream()
                .map(a -> {
                    boolean hasCv = cvRepository.findByUserId(a.getCandidate().getId())
                            .map(c -> c.getStatus() == CvStatus.PARSED)
                            .orElse(false);
                    return ApplicationDto.from(a, hasCv);
                })
                .toList();
    }

    @Transactional
    public ApplicationDto updateStatus(UUID applicationId, UpdateApplicationStatusRequest req) {
        User me = CurrentUser.requireRole(Role.RECRUITER);

        Application app = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new NotFoundException("Application not found"));

        if (!app.getJob().getCompany().getOwner().getId().equals(me.getId())) {
            throw new ValidationException("You do not own this job");
        }

        if (app.getStatus() == ApplicationStatus.WITHDRAWN) {
            throw new ValidationException("Candidate has withdrawn this application");
        }

        ApplicationStatus newStatus;
        try {
            newStatus = ApplicationStatus.valueOf(req.getStatus().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ValidationException("Invalid status: " + req.getStatus());
        }

        if (newStatus == ApplicationStatus.WITHDRAWN) {
            throw new ValidationException("Only the candidate can withdraw");
        }

        app.setStatus(newStatus);
        applicationRepository.save(app);
        log.info("Recruiter {} set application {} to {}", me.getId(), applicationId, newStatus);

        boolean hasCv = cvRepository.findByUserId(app.getCandidate().getId())
                .map(c -> c.getStatus() == CvStatus.PARSED)
                .orElse(false);
        return ApplicationDto.from(app, hasCv);
    }

    // =============== Shared ===============

    @Transactional(readOnly = true)
    public ApplicationDto getById(UUID applicationId) {
        User me = CurrentUser.get();

        Application app = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new NotFoundException("Application not found"));

        boolean isOwner = app.getCandidate().getId().equals(me.getId());
        boolean isRecruiter = app.getJob().getCompany().getOwner().getId().equals(me.getId());

        if (!isOwner && !isRecruiter) {
            throw new ValidationException("You do not have access to this application");
        }

        boolean hasCv = cvRepository.findByUserId(app.getCandidate().getId())
                .map(c -> c.getStatus() == CvStatus.PARSED)
                .orElse(false);
        return ApplicationDto.from(app, hasCv);
    }
}