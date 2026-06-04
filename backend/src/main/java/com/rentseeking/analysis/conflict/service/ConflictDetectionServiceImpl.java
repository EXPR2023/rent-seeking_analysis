package com.rentseeking.analysis.conflict.service;

import com.rentseeking.analysis.ai.service.AiService;
import com.rentseeking.analysis.auth.security.JwtUserPrincipal;
import com.rentseeking.analysis.auth.util.SecurityUtils;
import com.rentseeking.analysis.common.constant.ErrorCode;
import com.rentseeking.analysis.common.exception.BizException;
import com.rentseeking.analysis.common.response.PageResponse;
import com.rentseeking.analysis.conflict.dto.ConflictItemResponse;
import com.rentseeking.analysis.conflict.dto.ConflictItemReviewRequest;
import com.rentseeking.analysis.conflict.dto.ConflictTaskCreateRequest;
import com.rentseeking.analysis.conflict.dto.ConflictTaskDetailResponse;
import com.rentseeking.analysis.conflict.dto.ConflictTaskResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class ConflictDetectionServiceImpl implements ConflictDetectionService {

    private final JdbcTemplate jdbcTemplate;
    private final AiService aiService;

    public ConflictDetectionServiceImpl(JdbcTemplate jdbcTemplate, AiService aiService) {
        this.jdbcTemplate = jdbcTemplate;
        this.aiService = aiService;
    }

    @Override
    @Transactional
    public ConflictTaskDetailResponse createTask(ConflictTaskCreateRequest request) {
        JwtUserPrincipal principal = SecurityUtils.currentPrincipal();
        List<Long> compareIds = request.getCompareRegulationIds().stream()
                .filter(id -> !id.equals(request.getMainRegulationId()))
                .distinct()
                .toList();
        if (compareIds.isEmpty()) {
            throw new BizException(ErrorCode.BAD_REQUEST, "Please select at least one compare regulation");
        }
        RegulationSource main = loadRegulation(request.getMainRegulationId());
        List<RegulationSource> compares = compareIds.stream().map(this::loadRegulation).toList();
        if (compares.stream().anyMatch(compare -> !compare.regulationSetId.equals(main.regulationSetId))) {
            throw new BizException(ErrorCode.BAD_REQUEST, "Please compare regulations within the same regulation set");
        }

        KeyHolder keyHolder = new GeneratedKeyHolder();
        String compareIdText = String.join(",", compareIds.stream().map(String::valueOf).toList());
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement("""
                            INSERT INTO conflict_detection_task (
                              main_regulation_id, compare_regulation_ids, status, total_items, created_by
                            )
                            VALUES (?, ?, 'RUNNING', 0, ?)
                            """,
                    Statement.RETURN_GENERATED_KEYS
            );
            ps.setLong(1, request.getMainRegulationId());
            ps.setString(2, compareIdText);
            ps.setLong(3, principal.getUserId());
            return ps;
        }, keyHolder);
        Long taskId = keyHolder.getKey().longValue();

        int totalItems = 0;
        double threshold = similarityThreshold();
        for (RegulationSource compare : compares) {
            totalItems += detectMetadataConflicts(taskId, main, compare);
            totalItems += detectTextConflicts(taskId, main, compare, threshold);
        }
        jdbcTemplate.update("""
                        UPDATE conflict_detection_task
                        SET status = 'COMPLETED', total_items = ?, finished_at = CURRENT_TIMESTAMP
                        WHERE id = ?
                        """,
                totalItems,
                taskId
        );
        return getTask(taskId);
    }

    @Override
    public PageResponse<ConflictTaskResponse> listTasks(String status, Integer pageNo, Integer pageSize) {
        int currentPage = pageNo == null || pageNo < 1 ? 1 : pageNo;
        int size = pageSize == null || pageSize < 1 ? 10 : Math.min(pageSize, 100);
        StringBuilder condition = new StringBuilder(" WHERE 1 = 1");
        List<Object> args = new ArrayList<>();
        if (status != null && !status.isBlank()) {
            condition.append(" AND t.status = ?");
            args.add(status.trim());
        }
        Long total = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM conflict_detection_task t" + condition,
                Long.class,
                args.toArray()
        );
        List<Object> pageArgs = new ArrayList<>(args);
        pageArgs.add((currentPage - 1) * size);
        pageArgs.add(size);
        List<ConflictTaskResponse> records = jdbcTemplate.query("""
                        SELECT t.id, t.main_regulation_id, r.title AS main_regulation_title,
                               t.compare_regulation_ids, t.status, t.total_items,
                               u.real_name AS created_by_name, t.created_at, t.finished_at
                        FROM conflict_detection_task t
                        JOIN regulation r ON r.id = t.main_regulation_id
                        LEFT JOIN sys_user u ON u.id = t.created_by
                        %s
                        ORDER BY t.created_at DESC, t.id DESC
                        LIMIT ?, ?
                        """.formatted(condition),
                (rs, rowNum) -> mapTask(rs, new ConflictTaskResponse()),
                pageArgs.toArray()
        );
        return new PageResponse<>(records, currentPage, size, total == null ? 0 : total);
    }

    @Override
    public ConflictTaskDetailResponse getTask(Long id) {
        ConflictTaskDetailResponse detail = jdbcTemplate.query("""
                        SELECT t.id, t.main_regulation_id, r.title AS main_regulation_title,
                               t.compare_regulation_ids, t.status, t.total_items,
                               u.real_name AS created_by_name, t.created_at, t.finished_at
                        FROM conflict_detection_task t
                        JOIN regulation r ON r.id = t.main_regulation_id
                        LEFT JOIN sys_user u ON u.id = t.created_by
                        WHERE t.id = ?
                        """,
                rs -> rs.next() ? mapTask(rs, new ConflictTaskDetailResponse()) : null,
                id
        );
        if (detail == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "Conflict task not found");
        }
        detail.setItems(listItems(id));
        return detail;
    }

    @Override
    @Transactional
    public ConflictItemResponse confirmItem(Long id, ConflictItemReviewRequest request) {
        return updateItemStatus(id, "CONFIRMED", request);
    }

    @Override
    @Transactional
    public ConflictItemResponse ignoreItem(Long id, ConflictItemReviewRequest request) {
        return updateItemStatus(id, "IGNORED", request);
    }

    private int detectMetadataConflicts(Long taskId, RegulationSource main, RegulationSource compare) {
        int count = 0;
        if (isDifferent(main.applicableScope, compare.applicableScope)) {
            String explanation = aiService.explainConflict(taskId, "SCOPE_CONFLICT", main.applicableScope, compare.applicableScope);
            insertItem(taskId, main, compare, "SCOPE_CONFLICT", "MEDIUM", main.applicableScope, compare.applicableScope, null, explanation);
            count++;
        }
        if (isDifferent(main.publishDepartment, compare.publishDepartment)) {
            String explanation = aiService.explainConflict(taskId, "SUBJECT_CONFLICT", main.publishDepartment, compare.publishDepartment);
            insertItem(taskId, main, compare, "SUBJECT_CONFLICT", "LOW", main.publishDepartment, compare.publishDepartment, null, explanation);
            count++;
        }
        return count;
    }

    private int detectTextConflicts(Long taskId, RegulationSource main, RegulationSource compare, double threshold) {
        List<String> mainClauses = splitClauses(main.content);
        List<String> compareClauses = splitClauses(compare.content);
        double bestScore = 0;
        String bestMain = "";
        String bestCompare = "";
        for (String mainClause : mainClauses) {
            for (String compareClause : compareClauses) {
                double score = jaccard(mainClause, compareClause);
                if (score > bestScore) {
                    bestScore = score;
                    bestMain = mainClause;
                    bestCompare = compareClause;
                }
            }
        }
        if (bestScore >= threshold) {
            String type = bestScore >= 0.9 ? "DUPLICATE" : "INCONSISTENT";
            String level = bestScore >= 0.9 ? "HIGH" : "MEDIUM";
            BigDecimal similarity = BigDecimal.valueOf(bestScore).setScale(4, RoundingMode.HALF_UP);
            String explanation = aiService.explainConflict(taskId, type, bestMain, bestCompare);
            insertItem(taskId, main, compare, type, level, bestMain, bestCompare, similarity, explanation);
            return 1;
        }
        return 0;
    }

    private void insertItem(
            Long taskId,
            RegulationSource main,
            RegulationSource compare,
            String conflictType,
            String conflictLevel,
            String mainClause,
            String compareClause,
            BigDecimal similarity,
            String explanation
    ) {
        jdbcTemplate.update("""
                        INSERT INTO conflict_item (
                          task_id, main_regulation_id, compare_regulation_id, conflict_type, conflict_level,
                          main_clause, compare_clause, similarity, explanation, status
                        )
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 'PENDING')
                        """,
                taskId,
                main.id,
                compare.id,
                conflictType,
                conflictLevel,
                trimTo(mainClause, 4000),
                trimTo(compareClause, 4000),
                similarity,
                explanation
        );
    }

    private ConflictItemResponse updateItemStatus(Long id, String status, ConflictItemReviewRequest request) {
        int updated = jdbcTemplate.update("""
                        UPDATE conflict_item
                        SET status = ?, review_comment = ?, updated_at = CURRENT_TIMESTAMP
                        WHERE id = ?
                        """,
                status,
                request == null ? null : blankToNull(request.getReviewComment()),
                id
        );
        if (updated == 0) {
            throw new BizException(ErrorCode.NOT_FOUND, "Conflict item not found");
        }
        return getItem(id);
    }

    private ConflictItemResponse getItem(Long id) {
        ConflictItemResponse item = jdbcTemplate.query("""
                        SELECT i.id, i.task_id, i.main_regulation_id, i.compare_regulation_id,
                               cr.title AS compare_regulation_title, i.conflict_type, i.conflict_level,
                               i.main_clause, i.compare_clause, i.similarity, i.explanation,
                               i.status, i.review_comment, i.created_at, i.updated_at
                        FROM conflict_item i
                        JOIN regulation cr ON cr.id = i.compare_regulation_id
                        WHERE i.id = ?
                        """,
                rs -> rs.next() ? mapItem(rs) : null,
                id
        );
        if (item == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "Conflict item not found");
        }
        return item;
    }

    private List<ConflictItemResponse> listItems(Long taskId) {
        return jdbcTemplate.query("""
                        SELECT i.id, i.task_id, i.main_regulation_id, i.compare_regulation_id,
                               cr.title AS compare_regulation_title, i.conflict_type, i.conflict_level,
                               i.main_clause, i.compare_clause, i.similarity, i.explanation,
                               i.status, i.review_comment, i.created_at, i.updated_at
                        FROM conflict_item i
                        JOIN regulation cr ON cr.id = i.compare_regulation_id
                        WHERE i.task_id = ?
                        ORDER BY i.id
                        """,
                (rs, rowNum) -> mapItem(rs),
                taskId
        );
    }

    private RegulationSource loadRegulation(Long id) {
        RegulationSource source = jdbcTemplate.query("""
                        SELECT r.id, r.regulation_set_id, r.title, r.publish_department, r.applicable_scope,
                               COALESCE(rc.plain_text, rc.content, '') AS content
                        FROM regulation r
                        LEFT JOIN regulation_content rc ON rc.regulation_id = r.id
                        WHERE r.id = ? AND r.deleted = 0
                        """,
                rs -> {
                    if (!rs.next()) {
                        return null;
                    }
                    return new RegulationSource(
                            rs.getLong("id"),
                            rs.getLong("regulation_set_id"),
                            rs.getString("title"),
                            rs.getString("publish_department"),
                            rs.getString("applicable_scope"),
                            rs.getString("content")
                    );
                },
                id
        );
        if (source == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "Regulation not found");
        }
        return source;
    }

    private <T extends ConflictTaskResponse> T mapTask(ResultSet rs, T task) throws java.sql.SQLException {
        task.setId(rs.getLong("id"));
        task.setMainRegulationId(rs.getLong("main_regulation_id"));
        task.setMainRegulationTitle(rs.getString("main_regulation_title"));
        task.setCompareRegulationIds(rs.getString("compare_regulation_ids"));
        task.setStatus(rs.getString("status"));
        task.setTotalItems(rs.getInt("total_items"));
        task.setCreatedByName(rs.getString("created_by_name"));
        Timestamp createdAt = rs.getTimestamp("created_at");
        task.setCreatedAt(createdAt == null ? null : createdAt.toLocalDateTime());
        Timestamp finishedAt = rs.getTimestamp("finished_at");
        task.setFinishedAt(finishedAt == null ? null : finishedAt.toLocalDateTime());
        return task;
    }

    private ConflictItemResponse mapItem(ResultSet rs) throws java.sql.SQLException {
        ConflictItemResponse item = new ConflictItemResponse();
        item.setId(rs.getLong("id"));
        item.setTaskId(rs.getLong("task_id"));
        item.setMainRegulationId(rs.getLong("main_regulation_id"));
        item.setCompareRegulationId(rs.getLong("compare_regulation_id"));
        item.setCompareRegulationTitle(rs.getString("compare_regulation_title"));
        item.setConflictType(rs.getString("conflict_type"));
        item.setConflictLevel(rs.getString("conflict_level"));
        item.setMainClause(rs.getString("main_clause"));
        item.setCompareClause(rs.getString("compare_clause"));
        item.setSimilarity(rs.getBigDecimal("similarity"));
        item.setExplanation(rs.getString("explanation"));
        item.setStatus(rs.getString("status"));
        item.setReviewComment(rs.getString("review_comment"));
        Timestamp createdAt = rs.getTimestamp("created_at");
        item.setCreatedAt(createdAt == null ? null : createdAt.toLocalDateTime());
        Timestamp updatedAt = rs.getTimestamp("updated_at");
        item.setUpdatedAt(updatedAt == null ? null : updatedAt.toLocalDateTime());
        return item;
    }

    private List<String> splitClauses(String content) {
        if (content == null || content.isBlank()) {
            return List.of();
        }
        return List.of(content.split("[。；;\\n]")).stream()
                .map(String::trim)
                .filter(clause -> clause.length() >= 8)
                .limit(20)
                .toList();
    }

    private double jaccard(String left, String right) {
        Set<String> leftTokens = tokens(left);
        Set<String> rightTokens = tokens(right);
        if (leftTokens.isEmpty() || rightTokens.isEmpty()) {
            return 0;
        }
        Set<String> intersection = new LinkedHashSet<>(leftTokens);
        intersection.retainAll(rightTokens);
        Set<String> union = new LinkedHashSet<>(leftTokens);
        union.addAll(rightTokens);
        return (double) intersection.size() / union.size();
    }

    private Set<String> tokens(String text) {
        Set<String> result = new LinkedHashSet<>();
        String normalized = text == null ? "" : text.replaceAll("\\s+", "");
        for (int i = 0; i < normalized.length(); i++) {
            int end = Math.min(normalized.length(), i + 2);
            result.add(normalized.substring(i, end));
        }
        return result;
    }

    private double similarityThreshold() {
        String value = jdbcTemplate.query("""
                        SELECT config_value
                        FROM sys_config
                        WHERE config_key = 'conflict.similarity.threshold'
                        """,
                rs -> rs.next() ? rs.getString("config_value") : null
        );
        try {
            return value == null ? 0.75 : Double.parseDouble(value);
        } catch (NumberFormatException ex) {
            return 0.75;
        }
    }

    private boolean isDifferent(String left, String right) {
        return left != null && right != null && !left.isBlank() && !right.isBlank() && !left.trim().equals(right.trim());
    }

    private String trimTo(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private record RegulationSource(
            Long id,
            Long regulationSetId,
            String title,
            String publishDepartment,
            String applicableScope,
            String content
    ) {
    }
}
