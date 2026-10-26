package com.entrevio.entrevio_backend.service;

import com.entrevio.entrevio_backend.ai.InterviewPrepAgent;
import com.entrevio.entrevio_backend.dto.cv.ParsedCv;
import com.entrevio.entrevio_backend.dto.interview.InterviewPrepResponse;
import com.entrevio.entrevio_backend.dto.job.ParsedJob;
import com.entrevio.entrevio_backend.dto.matching.MatchBreakdown;
import com.entrevio.entrevio_backend.exception.NotFoundException;
import com.entrevio.entrevio_backend.exception.ValidationException;
import com.entrevio.entrevio_backend.model.Application;
import com.entrevio.entrevio_backend.model.CvDocument;
import com.entrevio.entrevio_backend.model.Job;
import com.entrevio.entrevio_backend.model.Match;
import com.entrevio.entrevio_backend.model.User;
import com.entrevio.entrevio_backend.model.enums.Role;
import com.entrevio.entrevio_backend.repository.ApplicationRepository;
import com.entrevio.entrevio_backend.repository.CvRepository;
import com.entrevio.entrevio_backend.repository.MatchRepository;
import com.entrevio.entrevio_backend.security.CurrentUser;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class InterviewPrepService {

    private static final Logger log = LoggerFactory.getLogger(InterviewPrepService.class);

    private final ApplicationRepository applicationRepository;
    private final CvRepository cvRepository;
    private final MatchRepository matchRepository;
    private final InterviewPrepAgent agent;
    private final ObjectMapper objectMapper;

    public InterviewPrepService(ApplicationRepository applicationRepository,
                                CvRepository cvRepository,
                                MatchRepository matchRepository,
                                InterviewPrepAgent agent,
                                ObjectMapper objectMapper) {
        this.applicationRepository = applicationRepository;
        this.cvRepository = cvRepository;
        this.matchRepository = matchRepository;
        this.agent = agent;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public InterviewPrepResponse prepare(UUID applicationId) {
        User me = CurrentUser.requireRole(Role.RECRUITER);

        Application app = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new NotFoundException("Application not found"));

        Job job = app.getJob();
        if (!job.getCompany().getOwner().getId().equals(me.getId())) {
            throw new ValidationException("You do not own this job");
        }

        User candidate = app.getCandidate();

        ParsedCv parsedCv = cvRepository.findByUserId(candidate.getId())
                .map(this::parseCv)
                .orElse(null);

        if (parsedCv == null) {
            throw new ValidationException("This candidate has no parsed CV yet");
        }

        ParsedJob parsedJob = parseJob(job);
        if (parsedJob == null) {
            throw new ValidationException("This job has no parsed data yet");
        }

        // Look up the deterministic match if it exists
        MatchBreakdown breakdown = null;
        Integer overallScore = null;
        Match match = matchRepository.findByJobIdAndCandidateId(job.getId(), candidate.getId())
                .orElse(null);
        if (match != null) {
            overallScore = match.getScore();
            breakdown = parseBreakdown(match);
        }

        String context = buildContext(app, candidate, parsedCv, job, parsedJob, overallScore, breakdown);
        log.info("Generating interview prep for application {}", applicationId);

        InterviewPrepResponse result = agent.generate(context);
        result.setCandidateName(candidate.getFullName());
        result.setJobTitle(job.getTitle());
        return result;
    }

    private String buildContext(Application app, User candidate, ParsedCv cv,
                                Job job, ParsedJob parsedJob,
                                Integer overallScore, MatchBreakdown breakdown) {
        StringBuilder sb = new StringBuilder();

        sb.append("=== CANDIDATE ===\n");
        sb.append("Name: ").append(candidate.getFullName()).append("\n");
        if (cv.getHeadline() != null) {
            sb.append("Headline: ").append(cv.getHeadline()).append("\n");
        }
        sb.append("Years of experience: ").append(cv.getYearsExperience()).append("\n");
        if (cv.getSkills() != null) {
            sb.append("Skills: ").append(String.join(", ", cv.getSkills())).append("\n");
        }
        if (cv.getEducation() != null && !cv.getEducation().isEmpty()) {
            sb.append("Education:\n");
            for (ParsedCv.Education e : cv.getEducation()) {
                sb.append("  - ").append(e.getDegree())
                        .append(" at ").append(e.getInstitution())
                        .append(" (").append(e.getYear()).append(")\n");
            }
        }

        sb.append("\n=== JOB ===\n");
        sb.append("Title: ").append(job.getTitle()).append("\n");
        if (parsedJob.getSummary() != null) {
            sb.append("Summary: ").append(parsedJob.getSummary()).append("\n");
        }
        sb.append("Required skills: ").append(String.join(", ", parsedJob.getRequiredSkills())).append("\n");
        if (parsedJob.getPreferredSkills() != null && !parsedJob.getPreferredSkills().isEmpty()) {
            sb.append("Preferred skills: ").append(String.join(", ", parsedJob.getPreferredSkills())).append("\n");
        }
        sb.append("Min years: ").append(parsedJob.getMinYearsExperience()).append("\n");
        sb.append("Seniority: ").append(parsedJob.getSeniority()).append("\n");

        if (overallScore != null) {
            sb.append("\n=== MATCH ANALYSIS (deterministic score) ===\n");
            sb.append("Overall score: ").append(overallScore).append("/100\n");
            if (breakdown != null) {
                sb.append("Skills: ").append(breakdown.getSkills()).append("/100\n");
                sb.append("Experience: ").append(breakdown.getExperience()).append("/100\n");
                sb.append("Education: ").append(breakdown.getEducation()).append("/100\n");
                sb.append("Seniority: ").append(breakdown.getSeniority()).append("/100\n");
            }
        }

        if (app.getCoverNote() != null && !app.getCoverNote().isBlank()) {
            sb.append("\n=== COVER NOTE ===\n").append(app.getCoverNote()).append("\n");
        }

        return sb.toString();
    }

    private ParsedCv parseCv(CvDocument doc) {
        if (doc.getParsedJson() == null) return null;
        try {
            return objectMapper.readValue(doc.getParsedJson(), ParsedCv.class);
        } catch (Exception e) {
            return null;
        }
    }

    private ParsedJob parseJob(Job job) {
        if (job.getParsedJson() == null) return null;
        try {
            return objectMapper.readValue(job.getParsedJson(), ParsedJob.class);
        } catch (Exception e) {
            return null;
        }
    }

    private MatchBreakdown parseBreakdown(Match match) {
        if (match.getBreakdownJson() == null) return null;
        try {
            return objectMapper.readValue(match.getBreakdownJson(), MatchBreakdown.class);
        } catch (Exception e) {
            return null;
        }
    }
}