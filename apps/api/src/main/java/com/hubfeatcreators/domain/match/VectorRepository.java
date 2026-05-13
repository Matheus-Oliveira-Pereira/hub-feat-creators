package com.hubfeatcreators.domain.match;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class VectorRepository {

    private final JdbcTemplate jdbc;

    public VectorRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void upsertBriefingEmbedding(UUID briefingId, float[] embedding) {
        jdbc.update(
                "UPDATE briefings SET embedding = ?::vector WHERE id = ?",
                toVectorString(embedding),
                briefingId);
    }

    public void upsertCreatorEmbedding(UUID influenciadorId, float[] embedding) {
        jdbc.update(
                "UPDATE creator_profile_features SET embedding = ?::vector WHERE influenciador_id = ?",
                toVectorString(embedding),
                influenciadorId);
    }

    public List<Map<String, Object>> findSimilarCreators(
            UUID assessoriaId, float[] queryEmbedding, int limit) {
        String sql =
                """
                SELECT cpf.influenciador_id,
                       1 - (cpf.embedding <=> ?::vector) AS cosine_sim
                FROM creator_profile_features cpf
                WHERE cpf.assessoria_id = ?
                  AND cpf.embedding IS NOT NULL
                  AND NOT EXISTS (
                      SELECT 1 FROM creator_match_optout o WHERE o.influenciador_id = cpf.influenciador_id
                  )
                ORDER BY cpf.embedding <=> ?::vector
                LIMIT ?
                """;
        String vec = toVectorString(queryEmbedding);
        return jdbc.queryForList(sql, vec, assessoriaId, vec, limit);
    }

    public List<Map<String, Object>> findSimilarBriefings(
            UUID assessoriaId, float[] queryEmbedding, int limit) {
        String sql =
                """
                SELECT b.id AS briefing_id,
                       b.prospeccao_id,
                       1 - (b.embedding <=> ?::vector) AS cosine_sim
                FROM briefings b
                WHERE b.assessoria_id = ?
                  AND b.embedding IS NOT NULL
                ORDER BY b.embedding <=> ?::vector
                LIMIT ?
                """;
        String vec = toVectorString(queryEmbedding);
        return jdbc.queryForList(sql, vec, assessoriaId, vec, limit);
    }

    public Double getCosineSimilarity(UUID briefingId, UUID influenciadorId) {
        String sql =
                """
                SELECT 1 - (b.embedding <=> cpf.embedding) AS sim
                FROM briefings b, creator_profile_features cpf
                WHERE b.id = ? AND cpf.influenciador_id = ?
                  AND b.embedding IS NOT NULL AND cpf.embedding IS NOT NULL
                """;
        List<Double> res = jdbc.queryForList(sql, Double.class, briefingId, influenciadorId);
        return res.isEmpty() ? null : res.get(0);
    }

    private String toVectorString(float[] v) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < v.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(v[i]);
        }
        sb.append("]");
        return sb.toString();
    }
}
