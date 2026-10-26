package com.entrevio.entrevio_backend.service;

import com.entrevio.entrevio_backend.dto.cv.ParsedCv;
import com.entrevio.entrevio_backend.dto.job.ParsedJob;
import com.entrevio.entrevio_backend.dto.matching.MatchBreakdown;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Deterministic candidate-to-job scoring.
 * No LLM. No randomness. Same inputs → same score, always.
 */
@Service
public class MatchingEngine {

    // Component weights (must sum to 1.0)
    private static final double W_SKILLS      = 0.50;
    private static final double W_EXPERIENCE  = 0.20;
    private static final double W_EDUCATION   = 0.15;
    private static final double W_SENIORITY   = 0.15;

    /** Compute a 0-100 score + per-component breakdown. */
    public Result score(ParsedCv cv, ParsedJob job) {
        int skills     = skillScore(cv, job);
        int experience = experienceScore(cv, job);
        int education  = educationScore(cv, job);
        int seniority  = seniorityScore(cv, job);

        int overall = (int) Math.round(
                skills     * W_SKILLS +
                        experience * W_EXPERIENCE +
                        education  * W_EDUCATION +
                        seniority  * W_SENIORITY
        );

        MatchBreakdown breakdown = MatchBreakdown.builder()
                .skills(skills)
                .experience(experience)
                .education(education)
                .seniority(seniority)
                .build();

        // Structured deltas for the explainer
        List<String> matched = new ArrayList<>();
        List<String> missing = new ArrayList<>();

        Set<String> cvSkills = normalizeSet(cv.getSkills());
        for (String req : safe(job.getRequiredSkills())) {
            if (cvSkills.contains(req.toLowerCase(Locale.ROOT))) {
                matched.add(req);
            } else {
                missing.add(req);
            }
        }

        return new Result(overall, breakdown, matched, missing);
    }

    // ---------- Component scorers ----------

    private int skillScore(ParsedCv cv, ParsedJob job) {
        Set<String> cvSkills = normalizeSet(cv.getSkills());
        List<String> required = safe(job.getRequiredSkills());
        List<String> preferred = safe(job.getPreferredSkills());

        if (required.isEmpty() && preferred.isEmpty()) return 60; // neutral

        int requiredHits = 0;
        for (String s : required) {
            if (cvSkills.contains(s.toLowerCase(Locale.ROOT))) requiredHits++;
        }

        int preferredHits = 0;
        for (String s : preferred) {
            if (cvSkills.contains(s.toLowerCase(Locale.ROOT))) preferredHits++;
        }

        double requiredRatio  = required.isEmpty()  ? 0 : (double) requiredHits  / required.size();
        double preferredRatio = preferred.isEmpty() ? 0 : (double) preferredHits / preferred.size();

        // Required weighted more than preferred
        double score = required.isEmpty()
                ? preferredRatio
                : (preferred.isEmpty() ? requiredRatio : requiredRatio * 0.8 + preferredRatio * 0.2);

        return clamp(score * 100);
    }

    private int experienceScore(ParsedCv cv, ParsedJob job) {
        Integer min = job.getMinYearsExperience();
        Integer years = cv.getYearsExperience();

        if (min == null || min == 0) return 80;   // no requirement → generous
        if (years == null)           return 30;   // unknown → penalize

        if (years >= min) {
            // Bonus up to +20 for up to 2x the requirement, capped at 100
            double bonus = Math.min((years - min) * 5.0, 20.0);
            return clamp(80 + bonus);
        }

        // Under-qualified: linear penalty
        double ratio = (double) years / min;
        return clamp(ratio * 80);
    }

    private int educationScore(ParsedCv cv, ParsedJob job) {
        int requiredRank = eduRank(job.getEducationLevel());
        int candidateRank = highestEduRank(cv);

        if (requiredRank == 0) return 80;      // no requirement
        if (candidateRank == 0) return 40;     // unknown

        if (candidateRank >= requiredRank) return 100;

        // Below requirement — partial credit
        return clamp((double) candidateRank / requiredRank * 70);
    }

    private int seniorityScore(ParsedCv cv, ParsedJob job) {
        int required = seniorityRank(job.getSeniority());
        int candidate = seniorityFromYears(cv.getYearsExperience());

        if (required == 0) return 80;

        int delta = Math.abs(required - candidate);
        return switch (delta) {
            case 0 -> 100;
            case 1 -> 75;
            case 2 -> 40;
            default -> 15;
        };
    }

    // ---------- Helpers ----------

    private Set<String> normalizeSet(List<String> raw) {
        Set<String> out = new HashSet<>();
        if (raw == null) return out;
        for (String s : raw) {
            if (s != null) out.add(s.toLowerCase(Locale.ROOT).trim());
        }
        return out;
    }

    private List<String> safe(List<String> list) {
        return list == null ? List.of() : list;
    }

    private int eduRank(String level) {
        if (level == null) return 0;
        return switch (level.toLowerCase(Locale.ROOT)) {
            case "phd" -> 4;
            case "master" -> 3;
            case "bachelor" -> 2;
            case "none" -> 1;
            default -> 0;
        };
    }

    /** Look at the CV's education list and rank its highest degree. */
    private int highestEduRank(ParsedCv cv) {
        if (cv.getEducation() == null || cv.getEducation().isEmpty()) return 0;
        int max = 0;
        for (ParsedCv.Education e : cv.getEducation()) {
            if (e == null || e.getDegree() == null) continue;
            String d = e.getDegree().toLowerCase(Locale.ROOT);
            int r = 1; // default: some education
            if (d.contains("phd") || d.contains("doctor")) r = 4;
            else if (d.contains("master") || d.contains("msc") || d.contains("mba")) r = 3;
            else if (d.contains("bachelor") || d.contains("bsc") || d.contains("licence")) r = 2;
            max = Math.max(max, r);
        }
        return max;
    }

    private int seniorityRank(String level) {
        if (level == null) return 0;
        return switch (level.toLowerCase(Locale.ROOT)) {
            case "lead" -> 4;
            case "senior" -> 3;
            case "mid" -> 2;
            case "junior" -> 1;
            default -> 0;
        };
    }

    private int seniorityFromYears(Integer years) {
        if (years == null) return 0;
        if (years >= 8) return 4;
        if (years >= 5) return 3;
        if (years >= 3) return 2;
        return 1;
    }

    private int clamp(double v) {
        return (int) Math.max(0, Math.min(100, Math.round(v)));
    }

    /** Result of a match: score + breakdown + matched/missing skills. */
    public record Result(
            int overall,
            MatchBreakdown breakdown,
            List<String> matchedSkills,
            List<String> missingSkills
    ) {}
}