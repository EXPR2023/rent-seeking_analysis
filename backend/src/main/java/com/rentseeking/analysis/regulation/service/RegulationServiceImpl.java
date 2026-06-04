package com.rentseeking.analysis.regulation.service;

import com.rentseeking.analysis.auth.security.JwtUserPrincipal;
import com.rentseeking.analysis.auth.util.SecurityUtils;
import com.rentseeking.analysis.common.constant.ErrorCode;
import com.rentseeking.analysis.common.exception.BizException;
import com.rentseeking.analysis.common.response.PageResponse;
import com.rentseeking.analysis.rag.service.KnowledgeRetrievalService;
import com.rentseeking.analysis.regulation.dto.RegulationContentRequest;
import com.rentseeking.analysis.regulation.dto.RegulationDetailResponse;
import com.rentseeking.analysis.regulation.dto.RegulationResponse;
import com.rentseeking.analysis.regulation.dto.RegulationSaveRequest;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;

@Service
public class RegulationServiceImpl implements RegulationService {

    private static final Set<String> REGULATION_TYPES = Set.of("POLICY", "PROCEDURE", "APPROVAL", "SUPERVISION");
    private static final Set<String> REGULATION_STATUSES = Set.of("DRAFT", "ACTIVE", "INACTIVE", "ARCHIVED");

    private final JdbcTemplate jdbcTemplate;
    private final KnowledgeRetrievalService knowledgeRetrievalService;

    public RegulationServiceImpl(JdbcTemplate jdbcTemplate, KnowledgeRetrievalService knowledgeRetrievalService) {
        this.jdbcTemplate = jdbcTemplate;
        this.knowledgeRetrievalService = knowledgeRetrievalService;
    }

    @Override
    public PageResponse<RegulationResponse> listRegulations(
            String keyword,
            Long regulationSetId,
            String typeCode,
            String status,
            Integer pageNo,
            Integer pageSize
    ) {
        int currentPage = pageNo == null || pageNo < 1 ? 1 : pageNo;
        int size = pageSize == null || pageSize < 1 ? 10 : Math.min(pageSize, 100);
        StringBuilder condition = new StringBuilder(" WHERE r.deleted = 0");
        List<Object> args = new ArrayList<>();
        if (keyword != null && !keyword.isBlank()) {
            condition.append(" AND (r.title LIKE ? OR r.code LIKE ? OR r.publish_department LIKE ?)");
            String pattern = "%" + keyword.trim() + "%";
            args.add(pattern);
            args.add(pattern);
            args.add(pattern);
        }
        if (regulationSetId != null) {
            condition.append(" AND r.regulation_set_id = ?");
            args.add(regulationSetId);
        }
        if (typeCode != null && !typeCode.isBlank()) {
            condition.append(" AND r.type_code = ?");
            args.add(typeCode.trim());
        }
        if (status != null && !status.isBlank()) {
            condition.append(" AND r.status = ?");
            args.add(status.trim());
        }

        Long total = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM regulation r" + condition,
                Long.class,
                args.toArray()
        );
        List<Object> pageArgs = new ArrayList<>(args);
        pageArgs.add((currentPage - 1) * size);
        pageArgs.add(size);
        List<RegulationResponse> records = jdbcTemplate.query("""
                        SELECT r.id, r.regulation_set_id, s.set_name AS regulation_set_name,
                               r.title, r.code, r.type_code, r.publish_department, r.effective_date,
                               r.expiry_date, r.applicable_scope, r.status, r.analysis_status,
                               cu.real_name AS created_by_name, r.created_at,
                               uu.real_name AS updated_by_name, r.updated_at
                        FROM regulation r
                        JOIN regulation_set s ON s.id = r.regulation_set_id AND s.deleted = 0
                        LEFT JOIN sys_user cu ON cu.id = r.created_by
                        LEFT JOIN sys_user uu ON uu.id = r.updated_by
                        %s
                        ORDER BY r.updated_at DESC, r.id DESC
                        LIMIT ?, ?
                        """.formatted(condition),
                (rs, rowNum) -> mapRegulation(rs, new RegulationResponse()),
                pageArgs.toArray()
        );
        return new PageResponse<>(records, currentPage, size, total == null ? 0 : total);
    }

    @Override
    @Transactional
    public RegulationDetailResponse createRegulation(RegulationSaveRequest request) {
        validateSaveRequest(request);
        JwtUserPrincipal principal = SecurityUtils.currentPrincipal();
        Long regulationSetId = resolveRegulationSetId(request.getRegulationSetId());
        KeyHolder keyHolder = new GeneratedKeyHolder();
        try {
            jdbcTemplate.update(connection -> {
                PreparedStatement ps = connection.prepareStatement("""
                                INSERT INTO regulation (
                                  regulation_set_id, title, code, type_code, publish_department, effective_date, expiry_date,
                                  applicable_scope, status, analysis_status, created_by, updated_by
                                )
                                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 'NOT_ANALYZED', ?, ?)
                                """,
                        Statement.RETURN_GENERATED_KEYS
                );
                ps.setLong(1, regulationSetId);
                ps.setString(2, request.getTitle().trim());
                ps.setString(3, blankToNull(request.getCode()));
                ps.setString(4, request.getTypeCode().trim());
                ps.setString(5, blankToNull(request.getPublishDepartment()));
                ps.setDate(6, toSqlDate(request.getEffectiveDate()));
                ps.setDate(7, toSqlDate(request.getExpiryDate()));
                ps.setString(8, blankToNull(request.getApplicableScope()));
                ps.setString(9, request.getStatus().trim());
                ps.setLong(10, principal.getUserId());
                ps.setLong(11, principal.getUserId());
                return ps;
            }, keyHolder);
        } catch (DuplicateKeyException ex) {
            throw new BizException(ErrorCode.CONFLICT, "Regulation code already exists");
        }
        Long id = keyHolder.getKey().longValue();
        if (request.getContent() != null && !request.getContent().isBlank()) {
            upsertContent(id, request.getContent());
            knowledgeRetrievalService.indexRegulation(id);
        }
        return getRegulation(id);
    }

    @Override
    public RegulationDetailResponse getRegulation(Long id) {
        RegulationDetailResponse detail = jdbcTemplate.query("""
                        SELECT r.id, r.regulation_set_id, s.set_name AS regulation_set_name,
                               r.title, r.code, r.type_code, r.publish_department, r.effective_date,
                               r.expiry_date, r.applicable_scope, r.status, r.analysis_status,
                               cu.real_name AS created_by_name, r.created_at,
                               uu.real_name AS updated_by_name, r.updated_at,
                               rc.content, rc.plain_text
                        FROM regulation r
                        JOIN regulation_set s ON s.id = r.regulation_set_id AND s.deleted = 0
                        LEFT JOIN sys_user cu ON cu.id = r.created_by
                        LEFT JOIN sys_user uu ON uu.id = r.updated_by
                        LEFT JOIN regulation_content rc ON rc.regulation_id = r.id
                        WHERE r.id = ? AND r.deleted = 0
                        """,
                rs -> rs.next() ? mapDetail(rs) : null,
                id
        );
        if (detail == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "Regulation not found");
        }
        return detail;
    }

    @Override
    @Transactional
    public RegulationDetailResponse updateRegulation(Long id, RegulationSaveRequest request) {
        ensureExists(id);
        validateSaveRequest(request);
        JwtUserPrincipal principal = SecurityUtils.currentPrincipal();
        Long regulationSetId = resolveRegulationSetId(request.getRegulationSetId());
        try {
            jdbcTemplate.update("""
                            UPDATE regulation
                            SET regulation_set_id = ?, title = ?, code = ?, type_code = ?, publish_department = ?, effective_date = ?,
                                expiry_date = ?, applicable_scope = ?, status = ?, updated_by = ?,
                                updated_at = CURRENT_TIMESTAMP
                            WHERE id = ? AND deleted = 0
                            """,
                    regulationSetId,
                    request.getTitle().trim(),
                    blankToNull(request.getCode()),
                    request.getTypeCode().trim(),
                    blankToNull(request.getPublishDepartment()),
                    toSqlDate(request.getEffectiveDate()),
                    toSqlDate(request.getExpiryDate()),
                    blankToNull(request.getApplicableScope()),
                    request.getStatus().trim(),
                    principal.getUserId(),
                    id
            );
        } catch (DuplicateKeyException ex) {
            throw new BizException(ErrorCode.CONFLICT, "Regulation code already exists");
        }
        if (request.getContent() != null) {
            upsertContent(id, request.getContent());
            knowledgeRetrievalService.indexRegulation(id);
            resetAnalysisStatus(id, principal.getUserId());
        }
        return getRegulation(id);
    }

    @Override
    @Transactional
    public RegulationDetailResponse saveContent(Long id, RegulationContentRequest request) {
        ensureExists(id);
        JwtUserPrincipal principal = SecurityUtils.currentPrincipal();
        upsertContent(id, request.getContent());
        knowledgeRetrievalService.indexRegulation(id);
        resetAnalysisStatus(id, principal.getUserId());
        return getRegulation(id);
    }

    @Override
    @Transactional
    public Boolean deleteRegulation(Long id) {
        ensureExists(id);
        JwtUserPrincipal principal = SecurityUtils.currentPrincipal();
        jdbcTemplate.update("""
                        UPDATE regulation
                        SET deleted = 1, updated_by = ?, updated_at = CURRENT_TIMESTAMP
                        WHERE id = ? AND deleted = 0
                        """,
                principal.getUserId(),
                id
        );
        return true;
    }

    private void validateSaveRequest(RegulationSaveRequest request) {
        if (!REGULATION_TYPES.contains(request.getTypeCode())) {
            throw new BizException(ErrorCode.BAD_REQUEST, "Invalid regulation type");
        }
        if (!REGULATION_STATUSES.contains(request.getStatus())) {
            throw new BizException(ErrorCode.BAD_REQUEST, "Invalid regulation status");
        }
        LocalDate effectiveDate = request.getEffectiveDate();
        LocalDate expiryDate = request.getExpiryDate();
        if (effectiveDate != null && expiryDate != null && expiryDate.isBefore(effectiveDate)) {
            throw new BizException(ErrorCode.BAD_REQUEST, "Expiry date cannot be earlier than effective date");
        }
    }

    private Long resolveRegulationSetId(Long regulationSetId) {
        if (regulationSetId != null) {
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM regulation_set WHERE id = ? AND enabled = 1 AND deleted = 0",
                    Integer.class,
                    regulationSetId
            );
            if (count == null || count == 0) {
                throw new BizException(ErrorCode.BAD_REQUEST, "Regulation set not found or disabled");
            }
            return regulationSetId;
        }
        Long defaultId = jdbcTemplate.query("""
                        SELECT id
                        FROM regulation_set
                        WHERE deleted = 0 AND enabled = 1
                        ORDER BY is_default DESC, updated_at DESC, id DESC
                        LIMIT 1
                        """,
                rs -> rs.next() ? rs.getLong("id") : null
        );
        if (defaultId == null) {
            throw new BizException(ErrorCode.BAD_REQUEST, "Please create a regulation set first");
        }
        return defaultId;
    }

    private void ensureExists(Long id) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM regulation WHERE id = ? AND deleted = 0",
                Integer.class,
                id
        );
        if (count == null || count == 0) {
            throw new BizException(ErrorCode.NOT_FOUND, "Regulation not found");
        }
    }

    private void upsertContent(Long id, String content) {
        String safeContent = content == null ? "" : content;
        String plainText = safeContent.replaceAll("<[^>]+>", " ").replaceAll("\\s+", " ").trim();
        jdbcTemplate.update("""
                        INSERT INTO regulation_content (regulation_id, content, plain_text, content_hash)
                        VALUES (?, ?, ?, ?)
                        ON DUPLICATE KEY UPDATE
                          content = VALUES(content),
                          plain_text = VALUES(plain_text),
                          content_hash = VALUES(content_hash),
                          updated_at = CURRENT_TIMESTAMP
                        """,
                id,
                safeContent,
                plainText,
                sha256(safeContent)
        );
    }

    private void resetAnalysisStatus(Long id, Long userId) {
        jdbcTemplate.update("""
                        UPDATE regulation
                        SET analysis_status = 'NOT_ANALYZED', updated_by = ?, updated_at = CURRENT_TIMESTAMP
                        WHERE id = ? AND deleted = 0
                        """,
                userId,
                id
        );
    }

    private RegulationDetailResponse mapDetail(ResultSet rs) throws java.sql.SQLException {
        RegulationDetailResponse detail = mapRegulation(rs, new RegulationDetailResponse());
        detail.setContent(rs.getString("content"));
        detail.setPlainText(rs.getString("plain_text"));
        return detail;
    }

    private <T extends RegulationResponse> T mapRegulation(ResultSet rs, T regulation) throws java.sql.SQLException {
        regulation.setId(rs.getLong("id"));
        regulation.setRegulationSetId(rs.getLong("regulation_set_id"));
        regulation.setRegulationSetName(rs.getString("regulation_set_name"));
        regulation.setTitle(rs.getString("title"));
        regulation.setCode(rs.getString("code"));
        regulation.setTypeCode(rs.getString("type_code"));
        regulation.setPublishDepartment(rs.getString("publish_department"));
        Date effectiveDate = rs.getDate("effective_date");
        regulation.setEffectiveDate(effectiveDate == null ? null : effectiveDate.toLocalDate());
        Date expiryDate = rs.getDate("expiry_date");
        regulation.setExpiryDate(expiryDate == null ? null : expiryDate.toLocalDate());
        regulation.setApplicableScope(rs.getString("applicable_scope"));
        regulation.setStatus(rs.getString("status"));
        regulation.setAnalysisStatus(rs.getString("analysis_status"));
        regulation.setCreatedByName(rs.getString("created_by_name"));
        Timestamp createdAt = rs.getTimestamp("created_at");
        regulation.setCreatedAt(createdAt == null ? null : createdAt.toLocalDateTime());
        regulation.setUpdatedByName(rs.getString("updated_by_name"));
        Timestamp updatedAt = rs.getTimestamp("updated_at");
        regulation.setUpdatedAt(updatedAt == null ? null : updatedAt.toLocalDateTime());
        return regulation;
    }

    private Date toSqlDate(LocalDate date) {
        return date == null ? null : Date.valueOf(date);
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
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
}
