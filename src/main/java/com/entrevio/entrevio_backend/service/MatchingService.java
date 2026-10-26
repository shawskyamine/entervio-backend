package com.entrevio.entrevio_backend.service;

import com.entrevio.entrevio_backend.ai.EvaluationAgent;
import com.entrevio.entrevio_backend.dto.cv.ParsedCv;
import com.entrevio.entrevio_backend.dto.job.ParsedJob;
import com.entrevio.entrevio_backend.dto.matching.MatchBreakdown;
import com.entrevio.entrevio_backend.dto.matching.MatchDto;
import com.entrevio.entrevio_backend.exception.NotFoundException;
import com.entrevio.entrevio_backend.exception.ValidationException;
import com.entrevio.entrevio_backend.model.CvDocument;
import com.entrevio.entrevio_backend.model.Job;
import com.entrevio.entrevio_backend.model.Match;
import com.entrevio.entrevio_backend.model.User;
import com.entrevio.entrevio_backend.model.enums.CvStatus;
import com.entrevio.entrevio_backend.model.enums.Role;
import com.entrevio.entrevio_backend.repository.CvRepository;
import com.entrevio.entrevio_backend.repository.JobRepository;
import com.entrevio.entrevio_backend.repository.MatchRepository;
import com.entrevio.entrevio_backend.security.CurrentUser;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
public class MatchingService {

    private static final Logger log = LoggerFactory.getLogger(MatchingService.class);

    /** How many top matches get an LLM explanation. */
    private static final int LLM_TOP_K = 10;

    private final JobRepository jobRepository;
    private final CvRepository cvRepository;
    private final MatchRepository matchRepository;
    private final MatchingEngine engine;
    private final EvaluationAgent evaluationAgent;
    private final ObjectMapper objectMapper;

    public MatchingService(JobRepository jobRepository,
                           CvRepository cvRepository,
                           MatchRepository matchRepository,
                           MatchingEngine engine,
                           EvaluationAgent evaluationAgent,
                           ObjectMapper objectMapper) {
        this.jobRepository = jobRepository;
        this.cvRepository = cvRepository;
        this.matchRepository = matchRepository;
        this.engine = engine;
        this.evaluationAgent = evaluationAgent;
        this.objectMapper = objectMapper;
    }

    /** Recompute matches for a job (idempotent). */
    @Transactional
    public List<MatchDto> runForJob(UUID jobId) {
        User me = CurrentUser.requireRole(Role.RECRUITER);

        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new NotFoundException("Job not found"));

        if (!job.getCompany().getOwner().getId().equals(me.getId())) {
            throw new ValidationException("You do not own this job");
        }

        ParsedJob parsedJob = parseJob(job);
        if (parsedJob == null) {
            throw new ValidationException("Job has no parsed data — re-post it");
        }

        // Fresh slate
        matchRepository.deleteByJobId(jobId);

        // Load all parsed CVs
        List<CvDocument> cvs = cvRepository.findByStatus(CvStatus.PARSED);
        if (cvs.isEmpty()) return List.of();

        // Deterministic scores for everyone
        List<Scored> scored = new ArrayList<>();
        for (CvDocument cv : cvs) {
            ParsedCv parsedCv = parseCv(cv);
            if (parsedCv == null) continue;
            MatchingEngine.Result r = engine.score(parsedCv, parsedJob);
            scored.add(new Scored(cv.getUser(), parsedCv, r));
        }

        // Rank descending
        scored.sort(Comparator.comparingInt((Scored s) -> s.result.overall()).reversed());

        // Persist — LLM explanation for top K, template for the rest
        List<Match> saved = new ArrayList<>();
        for (int i = 0; i < scored.size(); i++) {
            Scored s = scored.get(i);
            boolean useLlm = i < LLM_TOP_K;

            String explanation;
            boolean llmGenerated;
            if (useLlm) {
                try {
                    explanation = evaluationAgent.explain(
                            parsedJob,
                            s.parsedCv,
                            s.result.overall(),
                            s.result.matchedSkills(),
                            s.result.missingSkills()
                    );
                    llmGenerated = true;
                } catch (Exception e) {
                    log.warn("EvaluationAgent failed, falling back to template: {}", e.getMessage());
                    explanation = templateExplanation(s);
                    llmGenerated = false;
                }
            } else {
                explanation = templateExplanation(s);
                llmGenerated = false;
            }

            Match match = Match.builder()
                    .job(job)
                    .candidate(s.user)
                    .score(s.result.overall())
                    .breakdownJson(writeJson(s.result.breakdown()))
                    .explanation(explanation)
                    .llmGenerated(llmGenerated)
                    .build();
            saved.add(matchRepository.save(match));
        }

        return saved.stream().map(this::toDto).toList();
    }

    /** Fetch existing matches for a job (does NOT recompute). */
    @Transactional(readOnly = true)
    public List<MatchDto> listForJob(UUID jobId) {
        User me = CurrentUser.requireRole(Role.RECRUITER);

        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new NotFoundException("Job not found"));

        if (!job.getCompany().getOwner().getId().equals(me.getId())) {
            throw new ValidationException("You do not own this job");
        }

        return matchRepository.findByJobIdOrderByScoreDesc(jobId)
                .stream()
                .map(this::toDto)
                .toList();
    }

    /** Candidate sees their own matches across all jobs. */
    @Transactional(readOnly = true)
    public List<MatchDto> listMine() {
        User me = CurrentUser.requireRole(Role.CANDIDATE);
        // Filter the full match list to only those for this candidate.
        return matchRepository.findAll().stream()
                .filter(m -> m.getCandidate().getId().equals(me.getId()))
                .sorted(Comparator.comparingInt(Match::getScore).reversed())
                .map(this::toDto)
                .toList();
    }

    // ---------- Helpers ----------

    private ParsedJob parseJob(Job job) {
        if (job.getParsedJson() == null) return null;
        try {
            return objectMapper.readValue(job.getParsedJson(), ParsedJob.class);
        } catch (Exception e) {
            log.warn("Bad ParsedJob JSON: {}", e.getMessage());
            return null;
        }
    }

    private ParsedCv parseCv(CvDocument cv) {
        if (cv.getParsedJson() == null) return null;
        try {
            return objectMapper.readValue(cv.getParsedJson(), ParsedCv.class);
        } catch (Exception e) {
            log.warn("Bad ParsedCv JSON for user {}: {}", cv.getUser().getId(), e.getMessage());
            return null;
        }
    }

    private MatchBreakdown parseBreakdown(Match m) {
        if (m.getBreakdownJson() == null) return null;
        try {
            return objectMapper.readValue(m.getBreakdownJson(), MatchBreakdown.class);
        } catch (Exception e) {
            return null;
        }
    }

    private String writeJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return null;
        }
    }

    private MatchDto toDto(Match m) {
        return MatchDto.from(m, parseBreakdown(m));
    }

    private String templateExplanation(Scored s) {
        int score = s.result.overall();
        String level = score >= 80 ? "Strong" : score >= 60 ? "Moderate" : "Weak";
        int matched = s.result.matchedSkills().size();
        int missing = s.result.missingSkills().size();

        StringBuilder sb = new StringBuilder();
        sb.append(level).append(" match (").append(score).append("/100). ");
        if (matched > 0) {
            sb.append("Matches ").append(matched).append(" required skill")
                    .append(matched == 1 ? "" : "s");
        }
        if (missing > 0) {
            if (matched > 0) sb.append(", ");
            else sb.append("Missing ");
            sb.append("missing ").append(missing);
        }
        sb.append(".");
        return sb.toString();
    }

    private record Scored(User user, ParsedCv parsedCv, MatchingEngine.Result result) {}
}