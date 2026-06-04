package com.rentseeking.analysis.rag.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rentseeking.analysis.rag.dto.RagEvidence;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class KnowledgeRetrievalServiceImpl implements KnowledgeRetrievalService {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public KnowledgeRetrievalServiceImpl(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public void indexRegulation(Long regulationId) {
        RegulationKnowledgeSource source = jdbcTemplate.query("""
                        SELECT r.id, r.title, COALESCE(rc.plain_text, rc.content, '') AS content
                        FROM regulation r
                        LEFT JOIN regulation_content rc ON rc.regulation_id = r.id
                        WHERE r.id = ? AND r.deleted = 0
                        """,
                rs -> rs.next()
                        ? new RegulationKnowledgeSource(rs.getLong("id"), rs.getString("title"), rs.getString("content"))
                        : null,
                regulationId
        );
        if (source == null || source.content == null || source.content.isBlank()) {
            return;
        }
        jdbcTemplate.update("DELETE FROM knowledge_chunk WHERE source_type = 'REGULATION' AND source_id = ?", regulationId);
        List<String> chunks = splitChunks(source.content, chunkSize());
        List<Object[]> args = new ArrayList<>();
        for (int i = 0; i < chunks.size(); i++) {
            String chunk = chunks.get(i);
            double[] embedding = embed(source.title + "\n" + chunk);
            args.add(new Object[]{
                    "REGULATION",
                    regulationId,
                    source.title,
                    chunk,
                    i + 1,
                    sha256(chunk),
                    embeddingModel(),
                    embedding.length,
                    vectorToJson(embedding)
            });
        }
        jdbcTemplate.batchUpdate("""
                        INSERT INTO knowledge_chunk (
                          source_type, source_id, title, chunk_text, chunk_order, content_hash,
                          embedding_model, embedding_dimension, embedding_vector, indexed_at
                        )
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
                        """,
                args
        );
    }

    @Override
    @Transactional
    public int reindexAllRegulations() {
        List<Long> ids = jdbcTemplate.query("""
                        SELECT id
                        FROM regulation
                        WHERE deleted = 0
                        ORDER BY id
                        """,
                (rs, rowNum) -> rs.getLong("id")
        );
        for (Long id : ids) {
            indexRegulation(id);
        }
        return ids.size();
    }

    @Override
    public List<RagEvidence> retrieveForRiskAnalysis(Long regulationId, String queryText) {
        if (!ragEnabled()) {
            return List.of();
        }
        indexRegulation(regulationId);
        return retrieveIndexedEvidence(queryText, null, null);
    }

    @Override
    public List<RagEvidence> retrieveForCaseAnalysis(String queryText) {
        return retrieveForCaseAnalysis(queryText, null);
    }

    @Override
    public List<RagEvidence> retrieveForCaseAnalysis(String queryText, Long regulationSetId) {
        if (!ragEnabled()) {
            return List.of();
        }
        return retrieveIndexedEvidence(queryText, regulationSetId, null);
    }

    @Override
    public List<RagEvidence> retrieveForCaseAnalysisOutsideSet(String queryText, Long excludedRegulationSetId) {
        if (!ragEnabled()) {
            return List.of();
        }
        return retrieveIndexedEvidence(queryText, null, excludedRegulationSetId);
    }

    private List<RagEvidence> retrieveIndexedEvidence(String queryText, Long regulationSetId, Long excludedRegulationSetId) {
        indexMissingRegulations();
        double[] queryEmbedding = embed(queryText);
        StringBuilder setCondition = new StringBuilder();
        List<Object> args = new ArrayList<>();
        if (regulationSetId != null) {
            setCondition.append(" AND r.regulation_set_id = ?");
            args.add(regulationSetId);
        }
        if (excludedRegulationSetId != null) {
            setCondition.append(" AND r.regulation_set_id <> ?");
            args.add(excludedRegulationSetId);
        }
        List<KnowledgeChunk> chunks = jdbcTemplate.query("""
                        SELECT kc.id, kc.source_type, kc.source_id, kc.title, kc.chunk_text, kc.embedding_vector
                        FROM knowledge_chunk kc
                        JOIN regulation r ON r.id = kc.source_id AND r.deleted = 0
                        WHERE kc.source_type = 'REGULATION'
                          AND kc.embedding_vector IS NOT NULL
                          %s
                        ORDER BY kc.source_type, kc.source_id, kc.chunk_order
                        """.formatted(setCondition),
                (rs, rowNum) -> new KnowledgeChunk(
                        rs.getLong("id"),
                        rs.getString("source_type"),
                        rs.getLong("source_id"),
                        rs.getString("title"),
                        rs.getString("chunk_text"),
                        parseVector(rs.getString("embedding_vector"))
                ),
                args.toArray()
        );
        double threshold = similarityThreshold();
        int topK = topK();
        List<ScoredChunk> scored = chunks.stream()
                .map(chunk -> new ScoredChunk(chunk, cosine(queryEmbedding, chunk.embedding)))
                .filter(item -> item.score >= threshold)
                .sorted(Comparator.comparingDouble(ScoredChunk::score).reversed())
                .limit(topK)
                .toList();
        List<RagEvidence> evidences = new ArrayList<>();
        int citation = 1;
        for (ScoredChunk item : scored) {
            RagEvidence evidence = new RagEvidence();
            evidence.setChunkId(item.chunk.id);
            evidence.setSourceType(item.chunk.sourceType);
            evidence.setSourceId(item.chunk.sourceId);
            evidence.setTitle(item.chunk.title);
            evidence.setSnippet(item.chunk.text);
            evidence.setSimilarity(BigDecimal.valueOf(item.score).setScale(4, RoundingMode.HALF_UP));
            evidence.setCitationNo(citation++);
            evidences.add(evidence);
        }
        return evidences;
    }

    @Override
    public String formatContext(List<RagEvidence> evidences) {
        if (evidences == null || evidences.isEmpty()) {
            return "无可用 RAG 检索证据。";
        }
        StringBuilder builder = new StringBuilder();
        for (RagEvidence evidence : evidences) {
            builder.append("[证据")
                    .append(evidence.getCitationNo())
                    .append("] 来源=")
                    .append(evidence.getSourceType())
                    .append("#")
                    .append(evidence.getSourceId())
                    .append("，标题=")
                    .append(evidence.getTitle())
                    .append("，相似度=")
                    .append(evidence.getSimilarity())
                    .append("\n")
                    .append(evidence.getSnippet())
                    .append("\n\n");
        }
        return builder.toString().trim();
    }

    private List<String> splitChunks(String content, int targetSize) {
        List<String> clauses = List.of(content.split("[。；;\\n]")).stream()
                .map(String::trim)
                .filter(clause -> !clause.isBlank())
                .toList();
        List<String> chunks = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String clause : clauses) {
            String normalized = clause.endsWith("。") ? clause : clause + "。";
            if (!current.isEmpty() && current.length() + normalized.length() > targetSize) {
                chunks.add(current.toString());
                current.setLength(0);
            }
            current.append(normalized);
        }
        if (!current.isEmpty()) {
            chunks.add(current.toString());
        }
        return chunks.isEmpty() ? List.of(content) : chunks;
    }

    private double cosine(double[] left, double[] right) {
        if (left.length == 0 || right.length == 0 || left.length != right.length) {
            return 0;
        }
        double dot = 0;
        double leftNorm = 0;
        double rightNorm = 0;
        for (int i = 0; i < left.length; i++) {
            dot += left[i] * right[i];
            leftNorm += left[i] * left[i];
            rightNorm += right[i] * right[i];
        }
        if (leftNorm == 0 || rightNorm == 0) {
            return 0;
        }
        return dot / (Math.sqrt(leftNorm) * Math.sqrt(rightNorm));
    }

    private double[] embed(String text) {
        String baseUrl = configValue("embedding.base-url", "");
        String apiKey = configValue("embedding.api-key", "");
        String model = embeddingModel();
        boolean remoteConfigured = !baseUrl.isBlank() && !apiKey.isBlank() && !model.isBlank()
                && !"local-bigram-v1".equalsIgnoreCase(model);
        if ("openai-compatible".equalsIgnoreCase(configValue("embedding.provider", "local")) || remoteConfigured) {
            return remoteEmbedding(text);
        }
        return localEmbedding(text);
    }

    private double[] remoteEmbedding(String text) {
        String baseUrl = configValue("embedding.base-url", "");
        String apiKey = configValue("embedding.api-key", "");
        String model = embeddingModel();
        if (baseUrl.isBlank() || apiKey.isBlank() || model.isBlank()) {
            return localEmbedding(text);
        }
        for (String url : embeddingsUrls(baseUrl)) {
            try {
                String response = restClient().post()
                        .uri(url)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + apiKey)
                        .body("""
                                {
                                  "model": "%s",
                                  "input": %s
                                }
                                """.formatted(escapeJson(model), quoteJson(trimEmbeddingInput(text))))
                        .retrieve()
                        .body(String.class);
                JsonNode embedding = objectMapper.readTree(response)
                        .path("data")
                        .path(0)
                        .path("embedding");
                if (!embedding.isArray()) {
                    continue;
                }
                double[] vector = new double[embedding.size()];
                for (int i = 0; i < embedding.size(); i++) {
                    vector[i] = embedding.get(i).asDouble();
                }
                return vector;
            } catch (Exception ignored) {
                // Try the next compatible endpoint shape, then fall back to local vectors.
            }
        }
        return localEmbedding(text);
    }

    private double[] localEmbedding(String text) {
        int dimension = embeddingDimension();
        double[] vector = new double[dimension];
        for (String token : tokens(text)) {
            int hash = token.hashCode();
            int index = Math.floorMod(hash, dimension);
            vector[index] += hash >= 0 ? 1.0 : -1.0;
        }
        double norm = Math.sqrt(Arrays.stream(vector).map(value -> value * value).sum());
        if (norm == 0) {
            return vector;
        }
        for (int i = 0; i < vector.length; i++) {
            vector[i] = vector[i] / norm;
        }
        return vector;
    }

    private Set<String> tokens(String text) {
        Set<String> result = new LinkedHashSet<>();
        String normalized = (text == null ? "" : text).replaceAll("\\s+", "");
        for (int i = 0; i < normalized.length(); i++) {
            int end = Math.min(normalized.length(), i + 2);
            result.add(normalized.substring(i, end));
        }
        return result;
    }

    private boolean ragEnabled() {
        return Boolean.parseBoolean(configValue("rag.enabled", "true"));
    }

    private void indexMissingRegulations() {
        if (!Boolean.parseBoolean(configValue("rag.auto-index-missing", "true"))) {
            return;
        }
        List<Long> missingIds = jdbcTemplate.query("""
                        SELECT r.id
                        FROM regulation r
                        LEFT JOIN (
                          SELECT source_id
                          FROM knowledge_chunk
                          WHERE source_type = 'REGULATION'
                          GROUP BY source_id
                        ) indexed ON indexed.source_id = r.id
                        WHERE r.deleted = 0 AND indexed.source_id IS NULL
                        ORDER BY r.id
                        """,
                (rs, rowNum) -> rs.getLong("id")
        );
        for (Long id : missingIds) {
            indexRegulation(id);
        }
    }

    private int topK() {
        return parseInt(configValue("rag.top-k", "8"), 8, 1, 20);
    }

    private int chunkSize() {
        return parseInt(configValue("rag.chunk-size", "720"), 720, 120, 2000);
    }

    private double similarityThreshold() {
        try {
            return Math.max(0, Math.min(1, Double.parseDouble(configValue("rag.similarity.threshold", "0.10"))));
        } catch (NumberFormatException ex) {
            return 0.10;
        }
    }

    private String embeddingModel() {
        return configValue("embedding.model", "local-bigram-v1");
    }

    private int embeddingDimension() {
        return parseInt(configValue("embedding.dimension", "384"), 384, 32, 4096);
    }

    private Duration embeddingTimeout() {
        String value = configValue("embedding.timeout", "30s").trim().toLowerCase();
        try {
            if (value.endsWith("ms")) {
                return Duration.ofMillis(Long.parseLong(value.substring(0, value.length() - 2)));
            }
            if (value.endsWith("s")) {
                return Duration.ofSeconds(Long.parseLong(value.substring(0, value.length() - 1)));
            }
            return Duration.parse(value);
        } catch (Exception ex) {
            return Duration.ofSeconds(30);
        }
    }

    private List<String> embeddingsUrls(String baseUrl) {
        String normalized = (baseUrl == null ? "" : baseUrl.trim()).replaceAll("/+$", "");
        List<String> urls = new ArrayList<>();
        if (normalized.endsWith("/embeddings")) {
            urls.add(normalized);
            return urls;
        }
        String lower = normalized.toLowerCase();
        if (lower.contains("generativelanguage.googleapis.com") && !lower.contains("/openai")) {
            addUrl(urls, normalized + "/openai/embeddings");
        }
        if (!lower.endsWith("/v1") && !lower.contains("/v1/") && !lower.contains("/openai")) {
            addUrl(urls, normalized + "/v1/embeddings");
        }
        addUrl(urls, normalized + "/embeddings");
        return urls;
    }

    private void addUrl(List<String> urls, String url) {
        if (!url.isBlank() && !urls.contains(url)) {
            urls.add(url);
        }
    }

    private String trimEmbeddingInput(String text) {
        if (text == null) {
            return "";
        }
        int maxLength = parseInt(configValue("embedding.input.max-chars", "12000"), 12000, 1000, 100000);
        return text.length() <= maxLength ? text : text.substring(0, maxLength);
    }

    private RestClient restClient() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        Duration timeout = embeddingTimeout();
        requestFactory.setConnectTimeout(timeout);
        requestFactory.setReadTimeout(timeout);
        return RestClient.builder().requestFactory(requestFactory).build();
    }

    private int parseInt(String value, int defaultValue, int min, int max) {
        try {
            int parsed = Integer.parseInt(value);
            return Math.max(min, Math.min(max, parsed));
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }

    private String configValue(String key, String defaultValue) {
        String value = jdbcTemplate.query("""
                        SELECT config_value
                        FROM sys_config
                        WHERE config_key = ?
                        """,
                rs -> rs.next() ? rs.getString("config_value") : null,
                key
        );
        return value == null || value.isBlank() ? defaultValue : value;
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is not available", ex);
        }
    }

    private String vectorToJson(double[] vector) {
        try {
            return objectMapper.writeValueAsString(vector);
        } catch (Exception ex) {
            return "[]";
        }
    }

    private double[] parseVector(String value) {
        if (value == null || value.isBlank()) {
            return new double[0];
        }
        try {
            JsonNode root = objectMapper.readTree(value);
            if (!root.isArray()) {
                return new double[0];
            }
            double[] vector = new double[root.size()];
            for (int i = 0; i < root.size(); i++) {
                vector[i] = root.get(i).asDouble();
            }
            return vector;
        } catch (Exception ex) {
            return new double[0];
        }
    }

    private String quoteJson(String value) {
        try {
            return objectMapper.writeValueAsString(value == null ? "" : value);
        } catch (Exception ex) {
            return "\"\"";
        }
    }

    private String escapeJson(String value) {
        return (value == null ? "" : value).replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private record RegulationKnowledgeSource(Long id, String title, String content) {
    }

    private record KnowledgeChunk(Long id, String sourceType, Long sourceId, String title, String text, double[] embedding) {
    }

    private record ScoredChunk(KnowledgeChunk chunk, double score) {
    }
}
