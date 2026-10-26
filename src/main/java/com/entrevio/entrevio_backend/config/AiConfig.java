package com.entrevio.entrevio_backend.config;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.ollama.OllamaEmbeddingModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Two AI providers are on the classpath:
 *   - OpenAI starter (pointed at Groq) for chat/completion
 *   - Ollama starter for local embeddings
 *
 * Spring AI autoconfigures one bean of each type per provider, so injecting
 * ChatModel or EmbeddingModel by type is ambiguous. This config resolves it:
 *
 *   - ChatModel       -> OpenAI (Groq)   : CvAgent, JobAgent, EvaluationAgent, CvRagAnswerAgent
 *   - EmbeddingModel  -> Ollama          : EmbeddingAgent
 *
 * The other beans (ollamaChatModel, openAiEmbeddingModel) still get created
 * but nothing injects them, so they stay dormant.
 */
@Configuration
public class AiConfig {

    @Bean
    @Primary
    public ChatModel primaryChatModel(OpenAiChatModel openAiChatModel) {
        return openAiChatModel;
    }

    @Bean
    @Primary
    public EmbeddingModel primaryEmbeddingModel(OllamaEmbeddingModel ollamaEmbeddingModel) {
        return ollamaEmbeddingModel;
    }
}