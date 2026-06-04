package com.rentseeking.analysis.regulation.service;

import com.rentseeking.analysis.auth.security.JwtUserPrincipal;
import com.rentseeking.analysis.auth.util.SecurityUtils;
import com.rentseeking.analysis.common.constant.ErrorCode;
import com.rentseeking.analysis.common.exception.BizException;
import com.rentseeking.analysis.regulation.dto.RegulationSetRequest;
import com.rentseeking.analysis.regulation.dto.RegulationSetResponse;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

@Service
public class RegulationSetServiceImpl implements RegulationSetService {

    private final JdbcTemplate jdbcTemplate;

    public RegulationSetServiceImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<RegulationSetResponse> listSets(Boolean enabledOnly) {
        StringBuilder condition = new StringBuilder(" WHERE s.deleted = 0");
        List<Object> args = new ArrayList<>();
        if (Boolean.TRUE.equals(enabledOnly)) {
            condition.append(" AND s.enabled = 1");
        }
        return jdbcTemplate.query("""
                        SELECT s.id, s.set_name, s.set_code, s.description, s.enabled, s.is_default,
                               COALESCE(c.regulation_count, 0) AS regulation_count,
                               cu.real_name AS created_by_name, s.created_at,
                               uu.real_name AS updated_by_name, s.updated_at
                        FROM regulation_set s
                        LEFT JOIN (
                          SELECT regulation_set_id, COUNT(*) AS regulation_count
                          FROM regulation
                          WHERE deleted = 0
                          GROUP BY regulation_set_id
                        ) c ON c.regulation_set_id = s.id
                        LEFT JOIN sys_user cu ON cu.id = s.created_by
                        LEFT JOIN sys_user uu ON uu.id = s.updated_by
                        %s
                        ORDER BY s.is_default DESC, s.updated_at DESC, s.id DESC
                        """.formatted(condition),
                (rs, rowNum) -> mapSet(rs),
                args.toArray()
        );
    }

    @Override
    @Transactional
    public RegulationSetResponse createSet(RegulationSetRequest request) {
        JwtUserPrincipal principal = SecurityUtils.currentPrincipal();
        Long userId = principal.getUserId();
        if (Boolean.TRUE.equals(request.getDefaultSet())) {
            clearDefaultSet();
        }
        KeyHolder keyHolder = new GeneratedKeyHolder();
        try {
            jdbcTemplate.update(connection -> {
                PreparedStatement ps = connection.prepareStatement("""
                                INSERT INTO regulation_set (
                                  set_name, set_code, description, enabled, is_default, created_by, updated_by
                                )
                                VALUES (?, ?, ?, ?, ?, ?, ?)
                                """,
                        Statement.RETURN_GENERATED_KEYS
                );
                ps.setString(1, request.getSetName().trim());
                ps.setString(2, request.getSetCode().trim());
                ps.setString(3, blankToNull(request.getDescription()));
                ps.setInt(4, Boolean.FALSE.equals(request.getEnabled()) ? 0 : 1);
                ps.setInt(5, Boolean.TRUE.equals(request.getDefaultSet()) ? 1 : 0);
                ps.setLong(6, userId);
                ps.setLong(7, userId);
                return ps;
            }, keyHolder);
        } catch (DuplicateKeyException ex) {
            throw new BizException(ErrorCode.CONFLICT, "Regulation set code already exists");
        }
        return getSet(keyHolder.getKey().longValue());
    }

    @Override
    @Transactional
    public RegulationSetResponse updateSet(Long id, RegulationSetRequest request) {
        ensureExists(id);
        JwtUserPrincipal principal = SecurityUtils.currentPrincipal();
        if (Boolean.TRUE.equals(request.getDefaultSet())) {
            clearDefaultSet();
        }
        try {
            jdbcTemplate.update("""
                            UPDATE regulation_set
                            SET set_name = ?, set_code = ?, description = ?, enabled = ?, is_default = ?,
                                updated_by = ?, updated_at = CURRENT_TIMESTAMP
                            WHERE id = ? AND deleted = 0
                            """,
                    request.getSetName().trim(),
                    request.getSetCode().trim(),
                    blankToNull(request.getDescription()),
                    Boolean.FALSE.equals(request.getEnabled()) ? 0 : 1,
                    Boolean.TRUE.equals(request.getDefaultSet()) ? 1 : 0,
                    principal.getUserId(),
                    id
            );
        } catch (DuplicateKeyException ex) {
            throw new BizException(ErrorCode.CONFLICT, "Regulation set code already exists");
        }
        ensureOneDefault();
        return getSet(id);
    }

    @Override
    @Transactional
    public Boolean deleteSet(Long id) {
        ensureExists(id);
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM regulation WHERE regulation_set_id = ? AND deleted = 0",
                Integer.class,
                id
        );
        if (count != null && count > 0) {
            throw new BizException(ErrorCode.BAD_REQUEST, "Cannot delete a regulation set that still contains regulations");
        }
        JwtUserPrincipal principal = SecurityUtils.currentPrincipal();
        jdbcTemplate.update("""
                        UPDATE regulation_set
                        SET deleted = 1, enabled = 0, is_default = 0, updated_by = ?, updated_at = CURRENT_TIMESTAMP
                        WHERE id = ? AND deleted = 0
                        """,
                principal.getUserId(),
                id
        );
        ensureOneDefault();
        return true;
    }

    private RegulationSetResponse getSet(Long id) {
        RegulationSetResponse response = jdbcTemplate.query("""
                        SELECT s.id, s.set_name, s.set_code, s.description, s.enabled, s.is_default,
                               COALESCE(c.regulation_count, 0) AS regulation_count,
                               cu.real_name AS created_by_name, s.created_at,
                               uu.real_name AS updated_by_name, s.updated_at
                        FROM regulation_set s
                        LEFT JOIN (
                          SELECT regulation_set_id, COUNT(*) AS regulation_count
                          FROM regulation
                          WHERE deleted = 0
                          GROUP BY regulation_set_id
                        ) c ON c.regulation_set_id = s.id
                        LEFT JOIN sys_user cu ON cu.id = s.created_by
                        LEFT JOIN sys_user uu ON uu.id = s.updated_by
                        WHERE s.id = ? AND s.deleted = 0
                        """,
                rs -> rs.next() ? mapSet(rs) : null,
                id
        );
        if (response == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "Regulation set not found");
        }
        return response;
    }

    private void ensureExists(Long id) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM regulation_set WHERE id = ? AND deleted = 0",
                Integer.class,
                id
        );
        if (count == null || count == 0) {
            throw new BizException(ErrorCode.NOT_FOUND, "Regulation set not found");
        }
    }

    private void clearDefaultSet() {
        jdbcTemplate.update("UPDATE regulation_set SET is_default = 0 WHERE deleted = 0");
    }

    private void ensureOneDefault() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM regulation_set WHERE deleted = 0 AND enabled = 1 AND is_default = 1",
                Integer.class
        );
        if (count != null && count > 0) {
            return;
        }
        jdbcTemplate.update("""
                        UPDATE regulation_set
                        SET is_default = 1
                        WHERE deleted = 0 AND enabled = 1
                        ORDER BY updated_at DESC, id DESC
                        LIMIT 1
                        """);
    }

    private RegulationSetResponse mapSet(ResultSet rs) throws java.sql.SQLException {
        RegulationSetResponse response = new RegulationSetResponse();
        response.setId(rs.getLong("id"));
        response.setSetName(rs.getString("set_name"));
        response.setSetCode(rs.getString("set_code"));
        response.setDescription(rs.getString("description"));
        response.setEnabled(rs.getInt("enabled") == 1);
        response.setDefaultSet(rs.getInt("is_default") == 1);
        response.setRegulationCount(rs.getInt("regulation_count"));
        response.setCreatedByName(rs.getString("created_by_name"));
        Timestamp createdAt = rs.getTimestamp("created_at");
        response.setCreatedAt(createdAt == null ? null : createdAt.toLocalDateTime());
        response.setUpdatedByName(rs.getString("updated_by_name"));
        Timestamp updatedAt = rs.getTimestamp("updated_at");
        response.setUpdatedAt(updatedAt == null ? null : updatedAt.toLocalDateTime());
        return response;
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
