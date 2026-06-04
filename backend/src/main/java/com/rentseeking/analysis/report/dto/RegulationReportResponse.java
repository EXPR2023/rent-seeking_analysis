package com.rentseeking.analysis.report.dto;

import com.rentseeking.analysis.conflict.dto.ConflictItemResponse;
import com.rentseeking.analysis.regulation.dto.RegulationDetailResponse;
import com.rentseeking.analysis.risk.dto.RiskAnalysisDetailResponse;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class RegulationReportResponse {

    private Long reportId;
    private String reportName;
    private RegulationDetailResponse regulation;
    private RiskAnalysisDetailResponse latestRiskAnalysis;
    private List<ConflictItemResponse> conflictItems = new ArrayList<>();
    private String reportSummary;
    private LocalDateTime generatedAt;

    public Long getReportId() {
        return reportId;
    }

    public void setReportId(Long reportId) {
        this.reportId = reportId;
    }

    public String getReportName() {
        return reportName;
    }

    public void setReportName(String reportName) {
        this.reportName = reportName;
    }

    public RegulationDetailResponse getRegulation() {
        return regulation;
    }

    public void setRegulation(RegulationDetailResponse regulation) {
        this.regulation = regulation;
    }

    public RiskAnalysisDetailResponse getLatestRiskAnalysis() {
        return latestRiskAnalysis;
    }

    public void setLatestRiskAnalysis(RiskAnalysisDetailResponse latestRiskAnalysis) {
        this.latestRiskAnalysis = latestRiskAnalysis;
    }

    public List<ConflictItemResponse> getConflictItems() {
        return conflictItems;
    }

    public void setConflictItems(List<ConflictItemResponse> conflictItems) {
        this.conflictItems = conflictItems;
    }

    public String getReportSummary() {
        return reportSummary;
    }

    public void setReportSummary(String reportSummary) {
        this.reportSummary = reportSummary;
    }

    public LocalDateTime getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(LocalDateTime generatedAt) {
        this.generatedAt = generatedAt;
    }
}
