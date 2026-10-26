package com.entrevio.entrevio_backend.service;

import com.entrevio.entrevio_backend.ai.CvRagAnswerAgent;
import com.entrevio.entrevio_backend.ai.EmbeddingAgent;
import com.entrevio.entrevio_backend.dto.cv.ParsedCv;
import com.entrevio.entrevio_backend.dto.rag.CvRagSearchRequest;
import com.entrevio.entrevio_backend.dto.rag.CvRagSearchResponse;
import com.entrevio.entrevio_backend.model.CvDocument;
import com.entrevio.entrevio_backend.model.User;
import com.entrevio.entrevio_backend.model.enums.ChunkKind;
import com.entrevio.entrevio_backend.model.enums.CvStatus;
import com.entrevio.entrevio_backend.model.enums.Role;
import com.entrevio.entrevio_backend.repository.CvChunkRepository;
import com.entrevio.entrevio_backend.repository.CvRepository;
import com.entrevio.entrevio_backend.security.CurrentUser;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class CvRagService {

    private static final Logger log = LoggerFactory.getLogger(CvRagService.class);

    private final CvRepository cvRepository;
    private final CvChunkRepository chunkRepository;
    private final EmbeddingAgent embeddingAgent;
    private final CvRagAnswerAgent answerAgent;
    private final QueryKeywordExtractor keywordExtractor;
    private final ObjectMapper objectMapper;

    @Value("${entrevio.rag.chunk-size-tokens:128}")
    private int chunkSizeTokens;

    @Value("${entrevio.rag.chunk-overlap-tokens:20}")
    private int chunkOverlapTokens;

    @Value("${entrevio.rag.top-k:20}")
    private int defaultTopK;

    @Value("${entrevio.rag.max-chunks-per-candidate:2}")
    private int maxChunksPerCandidate;

    @Value("${entrevio.rag.max-candidates:5}")
    private int maxCandidates;

    @Value("${entrevio.rag.hybrid.vector-weight:0.6}")
    private double vectorWeight;

    @Value("${entrevio.rag.hybrid.keyword-weight:0.4}")
    private double keywordWeight;

    /** Chunks below this similarity are dropped before becoming citations. */
    @Value("${entrevio.rag.min-similarity:0.35}")
    private double minSimilarity;

    public CvRagService(CvRepository cvRepository,
                        CvChunkRepository chunkRepository,
                        EmbeddingAgent embeddingAgent,
                        CvRagAnswerAgent answerAgent,
                        QueryKeywordExtractor keywordExtractor,
                        ObjectMapper objectMapper) {
        this.cvRepository = cvRepository;
        this.chunkRepository = chunkRepository;
        this.embeddingAgent = embeddingAgent;
        this.answerAgent = answerAgent;
        this.keywordExtractor = keywordExtractor;
        this.objectMapper = objectMapper;
    }

    // =====================================================
    // Indexing
    // =====================================================

    @Transactional
    public void indexCv(CvDocument cv) {
        if (cv.getRawText() == null || cv.getRawText().isBlank()) {
            log.warn("Skipping indexing for CV {}: no raw text", cv.getId());
            return;
        }

        chunkRepository.deleteByCvId(cv.getId());

        List<ChunkDraft> drafts = buildChunks(cv);
        List<String> texts = drafts.stream().map(ChunkDraft::content).toList();
        List<float[]> vectors = embeddingAgent.embedAll(texts);

        for (int i = 0; i < drafts.size(); i++) {
            ChunkDraft d = drafts.get(i);
            String pgVector = embeddingAgent.toPgVectorLiteral(vectors.get(i));
            chunkRepository.insertChunk(
                    UUID.randomUUID(),
                    cv.getId(),
                    cv.getUser().getId(),
                    d.kind().name(),
                    d.index(),
                    d.content(),
                    d.tokenCount(),
                    pgVector
            );
        }

        log.info("Indexed CV {} → {} chunks", cv.getId(), drafts.size());
    }

    @Transactional
    public int reindexAll() {
        List<CvDocument> cvs = cvRepository.findByStatus(CvStatus.PARSED);
        int count = 0;
        for (CvDocument cv : cvs) {
            try {
                indexCv(cv);
                count++;
            } catch (Exception e) {
                log.warn("Failed to index CV {}: {}", cv.getId(), e.getMessage());
            }
        }
        return count;
    }

    private List<ChunkDraft> buildChunks(CvDocument cv) {
        List<ChunkDraft> drafts = new ArrayList<>();
        ParsedCv parsed = parseCv(cv);

        // 1) SKILLS — one dense chunk of all parsed skills
        if (parsed != null && parsed.getSkills() != null && !parsed.getSkills().isEmpty()) {
            String skills = "Skills: " + String.join(", ", parsed.getSkills());
            drafts.add(new ChunkDraft(ChunkKind.SKILLS, 0, skills, estimateTokens(skills)));
        }

        // 2) HEADER — synthetic summary; helps retrieval, hidden from UI
        String header = buildHeaderChunk(cv, parsed);
        if (!header.isBlank()) {
            drafts.add(new ChunkDraft(ChunkKind.HEADER, 1, header, estimateTokens(header)));
        }

        // 3) RAW — fixed-size slices of the actual CV text
        List<String> rawChunks = chunkText(cv.getRawText(), chunkSizeTokens, chunkOverlapTokens);
        int idx = 100;
        for (String c : rawChunks) {
            drafts.add(new ChunkDraft(ChunkKind.RAW, idx++, c, estimateTokens(c)));
        }

        return drafts;
    }

    private String buildHeaderChunk(CvDocument cv, ParsedCv parsed) {
        StringBuilder sb = new StringBuilder();
        sb.append("Candidate: ").append(cv.getUser().getFullName()).append("\n");
        if (parsed != null) {
            if (parsed.getHeadline() != null) {
                sb.append("Headline: ").append(parsed.getHeadline()).append("\n");
            }
            if (parsed.getYearsExperience() != null) {
                sb.append("Years of experience: ").append(parsed.getYearsExperience()).append("\n");
            }
            if (parsed.getSkills() != null && !parsed.getSkills().isEmpty()) {
                sb.append("Top skills: ")
                        .append(String.join(", ", parsed.getSkills().subList(
                                0, Math.min(10, parsed.getSkills().size()))))
                        .append("\n");
            }
        }
        return sb.toString().trim();
    }

    private List<String> chunkText(String text, int sizeTokens, int overlapTokens) {
        String[] tokens = text.trim().split("\\s+");
        List<String> chunks = new ArrayList<>();
        int step = Math.max(1, sizeTokens - overlapTokens);
        int i = 0;

        while (i < tokens.length) {
            int end = Math.min(tokens.length, i + sizeTokens);
            int extendedEnd = end;
            int maxExtension = Math.min(tokens.length, end + 40);
            for (int j = end; j < maxExtension; j++) {
                if (tokens[j].endsWith(".") || tokens[j].endsWith("!") || tokens[j].endsWith("?")) {
                    extendedEnd = j + 1;
                    break;
                }
            }
            String chunk = String.join(" ", java.util.Arrays.copyOfRange(tokens, i, extendedEnd));
            if (!chunk.isBlank()) chunks.add(chunk);
            if (extendedEnd >= tokens.length) break;
            i += step;
        }
        return chunks;
    }

    private int estimateTokens(String text) {
        return text.trim().split("\\s+").length;
    }

    private ParsedCv parseCv(CvDocument cv) {
        if (cv.getParsedJson() == null) return null;
        try {
            return objectMapper.readValue(cv.getParsedJson(), ParsedCv.class);
        } catch (Exception e) {
            return null;
        }
    }

    private record ChunkDraft(ChunkKind kind, int index, String content, int tokenCount) {}

    // =====================================================
    // Search
    // =====================================================

    @Transactional(readOnly = true)
    public CvRagSearchResponse search(CvRagSearchRequest req) {
        CurrentUser.requireRole(Role.RECRUITER);

        long t0 = System.currentTimeMillis();
        int k = req.getTopK() != null ? req.getTopK() : defaultTopK;

        // 1) Embed the query
        float[] queryVec = embeddingAgent.embedQuery(req.getQuery());
        String pgQuery = embeddingAgent.toPgVectorLiteral(queryVec);

        // 2) Extract keywords for BM25
        String keywords = keywordExtractor.extract(req.getQuery());

        // 3) Retrieve
        List<Object[]> rows;
        String mode;
        if (keywords.isBlank()) {
            rows = chunkRepository.searchNearestWithScore(pgQuery, k);
            mode = "vector";
        } else {
            rows = chunkRepository.searchHybrid(pgQuery, keywords, vectorWeight, keywordWeight, k);
            mode = "hybrid";
        }
        log.info("RAG search [{}] query='{}' keywords='{}' → {} chunks",
                mode, req.getQuery(), keywords, rows.size());

        // 4) Group by candidate — drop chunks below the similarity threshold
        Map<UUID, List<Hit>> byCandidate = new LinkedHashMap<>();
        int rejected = 0;
        for (Object[] row : rows) {
            UUID chunkId     = (UUID) row[0];
            UUID cvId        = (UUID) row[1];
            UUID candidateId = (UUID) row[2];
            String kind      = (String) row[3];
            int index        = ((Number) row[4]).intValue();
            String content   = (String) row[5];
            int tokenCount   = ((Number) row[6]).intValue();
            double score     = ((Number) row[7]).doubleValue();

            if (score < minSimilarity) {
                rejected++;
                continue;
            }

            byCandidate.computeIfAbsent(candidateId, x -> new ArrayList<>())
                    .add(new Hit(chunkId, cvId, kind, index, content, tokenCount, score));
        }
        log.info("RAG search — {} chunks total, {} below threshold ({}), {} candidate(s) kept",
                rows.size(), rejected, minSimilarity, byCandidate.size());

        // 5) Rank candidates by best chunk
        List<Map.Entry<UUID, List<Hit>>> ranked = byCandidate.entrySet().stream()
                .sorted((a, b) -> {
                    double sa = a.getValue().stream().mapToDouble(Hit::similarity).max().orElse(0);
                    double sb = b.getValue().stream().mapToDouble(Hit::similarity).max().orElse(0);
                    return Double.compare(sb, sa);
                })
                .limit(maxCandidates)
                .toList();

        // 6) Build citations + LLM snippets
        List<CvRagSearchResponse.Citation> citations = new ArrayList<>();
        List<CvRagAnswerAgent.Snippet> snippets = new ArrayList<>();

        int ref = 1;
        for (Map.Entry<UUID, List<Hit>> entry : ranked) {
            List<Hit> hits = entry.getValue().stream()
                    .sorted((a, b) -> Double.compare(b.similarity, a.similarity))
                    .limit(maxChunksPerCandidate)
                    .toList();
            if (hits.isEmpty()) continue;

            Hit top = hits.get(0);
            CvDocument cvDoc = cvRepository.findById(top.cvId()).orElse(null);
            if (cvDoc == null) continue;
            User candidate = cvDoc.getUser();
            ParsedCv parsed = parseCv(cvDoc);
            String headline = parsed != null ? parsed.getHeadline() : null;

            // ---- Text for the LLM (all chunks, labeled) ----
            StringBuilder combinedForLlm = new StringBuilder();
            for (int i = 0; i < hits.size(); i++) {
                Hit h = hits.get(i);
                if (i > 0) combinedForLlm.append("\n\n");
                String label = switch (h.kind()) {
                    case "SKILLS" -> "Skills";
                    case "RAW"    -> "Experience";
                    case "HEADER" -> "Overview";
                    default       -> "Section";
                };
                combinedForLlm.append("— ").append(label).append(" —\n");
                combinedForLlm.append(h.content());
            }
            String combinedText = combinedForLlm.toString().trim();

            snippets.add(new CvRagAnswerAgent.Snippet(
                    candidate.getFullName(), headline, combinedText));

            // ---- Structured data for the UI ----
            List<String> skillList = new ArrayList<>();
            for (Hit h : hits) {
                if ("SKILLS".equals(h.kind())) {
                    String cleaned = h.content().replaceFirst("(?i)^Skills:\\s*", "");
                    for (String s : cleaned.split(",")) {
                        String t = s.trim();
                        if (!t.isBlank()) skillList.add(t);
                    }
                    break;
                }
            }

            List<CvRagSearchResponse.EvidenceChunk> evidence = new ArrayList<>();
            for (Hit h : hits) {
                if (!"RAW".equals(h.kind())) continue;
                evidence.add(CvRagSearchResponse.EvidenceChunk.builder()
                        .content(h.content())
                        .similarity(round3(h.similarity()))
                        .tokenCount(h.tokenCount())
                        .build());
            }

            citations.add(CvRagSearchResponse.Citation.builder()
                    .ref(ref)
                    .candidateId(candidate.getId())
                    .candidateName(candidate.getFullName())
                    .candidateEmail(candidate.getEmail())
                    .headline(headline)
                    .cvId(top.cvId())
                    .chunkId(top.chunkId())
                    .snippet(truncate(combinedText, 700))
                    .similarity(round3(top.similarity()))
                    .skills(skillList)
                    .evidence(evidence)
                    .build());

            ref++;
        }

        // 7) LLM synthesis
        String answer;
        if (snippets.isEmpty()) {
            answer = "No candidates in the talent pool match this query.";
        } else {
            try {
                answer = answerAgent.answer(req.getQuery(), snippets);
            } catch (Exception e) {
                log.warn("Answer synthesis failed: {}", e.getMessage());
                answer = "Found " + snippets.size() + " relevant candidate(s). See citations below.";
            }
        }

        long elapsed = System.currentTimeMillis() - t0;
        return CvRagSearchResponse.builder()
                .answer(answer)
                .citations(citations)
                .totalChunksSearched(rows.size())
                .searchTimeMs(elapsed)
                .build();
    }

    private String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max).trim() + "…";
    }

    private double round3(double v) {
        return Math.round(v * 1000.0) / 1000.0;
    }

    private record Hit(UUID chunkId, UUID cvId, String kind, int index,
                       String content, int tokenCount, double similarity) {}
}