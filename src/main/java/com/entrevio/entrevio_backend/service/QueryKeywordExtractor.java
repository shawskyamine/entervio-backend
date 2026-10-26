package com.entrevio.entrevio_backend.service;

import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Turns a natural-language query into a BM25 keyword expression.
 *
 * Example:
 *   "candidate with knowledge in angular"
 *      -> "angular"
 *
 *   "senior Java developers with Spring Boot"
 *      -> "java | spring | boot"
 *
 * The extractor strips stopwords (English + French), short tokens, and
 * punctuation. The output is joined with " | " so Postgres's to_tsquery
 * treats it as OR.
 */
@Service
public class QueryKeywordExtractor {

    private static final Set<String> STOPWORDS = Set.of(
            // English connectors and filler
            "a", "an", "the", "in", "on", "at", "of", "to", "for", "with",
            "and", "or", "but", "is", "are", "was", "were", "be", "been",
            "who", "whom", "that", "which", "what", "when", "where",
            "has", "have", "had", "do", "does", "did",
            "me", "my", "we", "our", "you", "your", "his", "her", "their",
            "this", "these", "those",

            // Query-shaped filler
            "candidate", "candidates", "person", "people", "someone",
            "developer", "developers", "engineer", "engineers",
            "knowledge", "experience", "experienced", "skilled", "skills",
            "looking", "find", "show", "give", "need", "want",
            "using", "used", "work", "worked", "works",

            // French connectors and filler
            "de", "du", "des", "la", "le", "les", "et", "avec", "en",
            "pour", "qui", "que", "un", "une", "dans", "sur", "au", "aux",
            "ce", "cet", "cette", "ces", "par", "vers", "chez",
            "candidat", "candidats", "connaissance", "connaissances",
            "compétence", "compétences", "expérience",
            "développeur", "développeurs", "ingénieur", "ingénieurs"
    );

    /**
     * @return a Postgres tsquery string like "angular" or "java | spring"
     *         Returns "" if no useful keywords remain.
     */
    public String extract(String query) {
        if (query == null || query.isBlank()) return "";

        String[] tokens = query.toLowerCase(Locale.ROOT)
                // keep letters (incl. accented), digits, and a few tech chars
                .replaceAll("[^a-z0-9à-ÿ.#+\\s]", " ")
                .split("\\s+");

        List<String> keywords = Arrays.stream(tokens)
                .map(String::trim)
                .filter(t -> t.length() >= 2)
                .filter(t -> !STOPWORDS.contains(t))
                .distinct()
                .toList();

        if (keywords.isEmpty()) return "";
        return String.join(" | ", keywords);
    }
}