package com.rentseeking.analysis.dashboard.service;

import com.rentseeking.analysis.dashboard.dto.ChartItemResponse;
import com.rentseeking.analysis.dashboard.dto.DashboardSummaryResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DashboardService {

    private final JdbcTemplate jdbcTemplate;

    public DashboardService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public DashboardSummaryResponse summary() {
        DashboardSummaryResponse response = new DashboardSummaryResponse();
        response.setRegulationTotal(count("SELECT COUNT(*) FROM regulation WHERE deleted = 0"));
        response.setAnalyzedRegulationTotal(count("SELECT COUNT(*) FROM regulation WHERE deleted = 0 AND analysis_status = 'COMPLETED'"));
        response.setConflictTotal(count("SELECT COUNT(*) FROM conflict_item"));
        response.setPendingConflictTotal(count("SELECT COUNT(*) FROM conflict_item WHERE status = 'PENDING'"));
        response.setHighRiskTotal(count("SELECT COUNT(*) FROM risk_analysis WHERE risk_level IN ('HIGH', 'CRITICAL')"));
        response.setSuggestionTotal(count("SELECT COUNT(*) FROM risk_item WHERE suggestion IS NOT NULL AND suggestion <> ''"));
        return response;
    }

    public List<ChartItemResponse> riskLevels() {
        return jdbcTemplate.query("""
                        SELECT d.item_code, d.item_name, COUNT(a.id) AS total
                        FROM sys_dict d
                        LEFT JOIN risk_analysis a ON a.risk_level = d.item_code
                        WHERE d.dict_type = 'RISK_LEVEL' AND d.enabled = 1
                        GROUP BY d.item_code, d.item_name, d.sort_order
                        ORDER BY d.sort_order
                        """,
                (rs, rowNum) -> new ChartItemResponse(
                        rs.getString("item_name"),
                        rs.getString("item_code"),
                        rs.getLong("total")
                )
        );
    }

    public List<ChartItemResponse> conflictStatus() {
        return jdbcTemplate.query("""
                        SELECT d.item_code, d.item_name, COUNT(i.id) AS total
                        FROM sys_dict d
                        LEFT JOIN conflict_item i ON i.status = d.item_code
                        WHERE d.dict_type = 'CONFLICT_STATUS' AND d.enabled = 1
                        GROUP BY d.item_code, d.item_name, d.sort_order
                        ORDER BY d.sort_order
                        """,
                (rs, rowNum) -> new ChartItemResponse(
                        rs.getString("item_name"),
                        rs.getString("item_code"),
                        rs.getLong("total")
                )
        );
    }

    private Long count(String sql) {
        Long value = jdbcTemplate.queryForObject(sql, Long.class);
        return value == null ? 0 : value;
    }
}
