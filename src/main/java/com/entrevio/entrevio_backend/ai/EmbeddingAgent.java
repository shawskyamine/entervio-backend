package com.entrevio.entrevio_backend.ai;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EmbeddingAgent {

    private static final String DOC_PREFIX = "search_document: ";
    private static final String QUERY_PREFIX = "search_query: ";

    private final EmbeddingModel embeddingModel;

    public EmbeddingAgent(EmbeddingModel embeddingModel) {
        this.embeddingModel = embeddingModel;
    }

    public float[] embedDocument(String text) {
        return embeddingModel.embed(DOC_PREFIX + text);
    }

    public List<float[]> embedDocuments(List<String> texts) {
        return embeddingModel.embed(texts.stream().map(t -> DOC_PREFIX + t).toList());
    }

    public float[] embedQuery(String query) {
        return embeddingModel.embed(QUERY_PREFIX + query);
    }

    // Back-compat aliases
    public float[] embed(String text) { return embedDocument(text); }
    public List<float[]> embedAll(List<String> texts) { return embedDocuments(texts); }

    public String toPgVectorLiteral(float[] vector) {
        StringBuilder sb = new StringBuilder();
        sb.append('[');
        for (int i = 0; i < vector.length; i++) {
            if (i > 0) sb.append(',');
            sb.append(vector[i]);
        }
        sb.append(']');
        return sb.toString();
    }
}