package com.rentseeking.analysis.report.service;

import com.rentseeking.analysis.auth.security.JwtUserPrincipal;
import com.rentseeking.analysis.auth.util.SecurityUtils;
import com.rentseeking.analysis.conflict.dto.ConflictItemResponse;
import com.rentseeking.analysis.regulation.dto.RegulationDetailResponse;
import com.rentseeking.analysis.regulation.service.RegulationService;
import com.rentseeking.analysis.report.dto.RegulationReportResponse;
import com.rentseeking.analysis.risk.dto.RiskAnalysisDetailResponse;
import com.rentseeking.analysis.risk.service.RiskAnalysisService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class ReportService {

    private final JdbcTemplate jdbcTemplate;
    private final RegulationService regulationService;
    private final RiskAnalysisService riskAnalysisService;

    public ReportService(JdbcTemplate jdbcTemplate, RegulationService regulationService, RiskAnalysisService riskAnalysisService) {
        this.jdbcTemplate = jdbcTemplate;
        this.regulationService = regulationService;
        this.riskAnalysisService = riskAnalysisService;
    }

    public RegulationReportResponse regulationReport(Long regulationId) {
        RegulationDetailResponse regulation = regulationService.getRegulation(regulationId);
        RiskAnalysisDetailResponse risk = latestRisk(regulationId);
        List<ConflictItemResponse> conflicts = conflictItems(regulationId);
        RegulationReportResponse response = new RegulationReportResponse();
        response.setReportName("制度分析报告 - " + regulation.getTitle());
        response.setRegulation(regulation);
        response.setLatestRiskAnalysis(risk);
        response.setConflictItems(conflicts);
        response.setGeneratedAt(LocalDateTime.now());
        response.setReportSummary(summary(regulation, risk, conflicts));
        response.setReportId(saveSnapshot(response, regulationId));
        return response;
    }

    private RiskAnalysisDetailResponse latestRisk(Long regulationId) {
        Long id = jdbcTemplate.query("""
                        SELECT id
                        FROM risk_analysis
                        WHERE regulation_id = ?
                        ORDER BY created_at DESC, id DESC
                        LIMIT 1
                        """,
                rs -> rs.next() ? rs.getLong("id") : null,
                regulationId
        );
        return id == null ? null : riskAnalysisService.getAnalysis(id);
    }

    private List<ConflictItemResponse> conflictItems(Long regulationId) {
        return jdbcTemplate.query("""
                        SELECT i.id, i.task_id, i.main_regulation_id, i.compare_regulation_id,
                               cr.title AS compare_regulation_title, i.conflict_type, i.conflict_level,
                               i.main_clause, i.compare_clause, i.similarity, i.explanation,
                               i.status, i.review_comment, i.created_at, i.updated_at
                        FROM conflict_item i
                        JOIN regulation cr ON cr.id = i.compare_regulation_id
                        WHERE i.main_regulation_id = ? OR i.compare_regulation_id = ?
                        ORDER BY i.created_at DESC, i.id DESC
                        LIMIT 20
                        """,
                (rs, rowNum) -> {
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
                    BigDecimal similarity = rs.getBigDecimal("similarity");
                    item.setSimilarity(similarity);
                    item.setExplanation(rs.getString("explanation"));
                    item.setStatus(rs.getString("status"));
                    item.setReviewComment(rs.getString("review_comment"));
                    Timestamp createdAt = rs.getTimestamp("created_at");
                    item.setCreatedAt(createdAt == null ? null : createdAt.toLocalDateTime());
                    Timestamp updatedAt = rs.getTimestamp("updated_at");
                    item.setUpdatedAt(updatedAt == null ? null : updatedAt.toLocalDateTime());
                    return item;
                },
                regulationId,
                regulationId
        );
    }

    private String summary(RegulationDetailResponse regulation, RiskAnalysisDetailResponse risk, List<ConflictItemResponse> conflicts) {
        String riskText = risk == null ? "暂未形成风险分析结果" : "最新风险等级为 " + risk.getRiskLevel() + "，风险分数 " + risk.getRiskScore();
        return "《" + regulation.getTitle() + "》报告已聚合制度正文、冲突检测结果与风险分析结果。"
                + riskText + "，关联冲突项 " + conflicts.size() + " 个。";
    }

    private Long saveSnapshot(RegulationReportResponse response, Long regulationId) {
        JwtUserPrincipal principal = SecurityUtils.currentPrincipal();
        KeyHolder keyHolder = new GeneratedKeyHolder();
        String snapshot = response.getReportSummary();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement("""
                            INSERT INTO report_record (report_name, report_type, regulation_id, content_snapshot, created_by)
                            VALUES (?, 'REGULATION', ?, ?, ?)
                            """,
                    Statement.RETURN_GENERATED_KEYS
            );
            ps.setString(1, response.getReportName());
            ps.setLong(2, regulationId);
            ps.setString(3, snapshot);
            ps.setLong(4, principal.getUserId());
            return ps;
        }, keyHolder);
        return keyHolder.getKey().longValue();
    }
}
