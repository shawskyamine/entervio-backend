package com.entrevio.entrevio_backend.repository;

import com.entrevio.entrevio_backend.model.CvChunk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CvChunkRepository extends JpaRepository<CvChunk, UUID> {

    List<CvChunk> findByCvId(UUID cvId);

    @Modifying
    @Query("DELETE FROM CvChunk c WHERE c.cv.id = :cvId")
    void deleteByCvId(@Param("cvId") UUID cvId);

    long countByCvId(UUID cvId);

    @Modifying
    @Query(value = """
            INSERT INTO cv_chunks
              (id, cv_id, candidate_id, kind, chunk_index, content, token_count, embedding, created_at)
            VALUES
              (:id, :cvId, :candidateId, :kind, :chunkIndex, :content, :tokenCount,
               CAST(:embedding AS vector), now())
            """, nativeQuery = true)
    void insertChunk(
            @Param("id") UUID id,
            @Param("cvId") UUID cvId,
            @Param("candidateId") UUID candidateId,
            @Param("kind") String kind,
            @Param("chunkIndex") int chunkIndex,
            @Param("content") String content,
            @Param("tokenCount") int tokenCount,
            @Param("embedding") String embedding
    );

    /**
     * Pure vector search — used as a fallback when the query has no
     * useful keywords (e.g. only stopwords).
     */
    @Query(value = """
            SELECT c.id,
                   c.cv_id,
                   c.candidate_id,
                   c.kind,
                   c.chunk_index,
                   c.content,
                   c.token_count,
                   1 - (c.embedding <=> CAST(:query AS vector)) AS similarity
            FROM cv_chunks c
            WHERE c.embedding IS NOT NULL
            ORDER BY c.embedding <=> CAST(:query AS vector)
            LIMIT :k
            """, nativeQuery = true)
    List<Object[]> searchNearestWithScore(
            @Param("query") String queryVector,
            @Param("k") int k
    );

    /**
     * Hybrid search — combines vector similarity and BM25 keyword rank.
     *
     *  - vec: top 50 chunks by cosine similarity
     *  - kw:  top 50 chunks by ts_rank_cd (BM25-ish), normalized 0..1
     *  - final: vec_score * vecWeight + kw_score * kwWeight
     *
     * Returned column order must match how CvRagService unpacks Object[].
     */
    @Query(value = """
            WITH vec AS (
                SELECT id,
                       1 - (embedding <=> CAST(:queryVec AS vector)) AS score
                FROM cv_chunks
                WHERE embedding IS NOT NULL
                ORDER BY embedding <=> CAST(:queryVec AS vector)
                LIMIT 50
            ),
            kw AS (
                SELECT id,
                       ts_rank_cd(content_tsv, to_tsquery('simple', :keywords)) AS raw,
                       MAX(ts_rank_cd(content_tsv, to_tsquery('simple', :keywords)))
                           OVER () AS max_raw
                FROM cv_chunks
                WHERE content_tsv @@ to_tsquery('simple', :keywords)
                ORDER BY raw DESC
                LIMIT 50
            )
            SELECT c.id,
                   c.cv_id,
                   c.candidate_id,
                   c.kind,
                   c.chunk_index,
                   c.content,
                   c.token_count,
                   (
                       COALESCE(v.score, 0) * :vecWeight
                     + COALESCE(kw.raw / NULLIF(kw.max_raw, 0), 0) * :kwWeight
                   ) AS combined
            FROM cv_chunks c
            LEFT JOIN vec v  ON v.id  = c.id
            LEFT JOIN kw     ON kw.id = c.id
            WHERE v.id IS NOT NULL OR kw.id IS NOT NULL
            ORDER BY combined DESC
            LIMIT :k
            """, nativeQuery = true)
    List<Object[]> searchHybrid(
            @Param("queryVec") String queryVector,
            @Param("keywords") String keywords,
            @Param("vecWeight") double vecWeight,
            @Param("kwWeight") double kwWeight,
            @Param("k") int k
    );
}