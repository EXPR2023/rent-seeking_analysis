package com.rentseeking.analysis.risk.dto;

public class RiskItemResponse {

    private Long id;
    private Long analysisId;
    private String indicatorCode;
    private String title;
    private String description;
    private String reason;
    private String suggestion;
    private String relatedClause;
    private String riskLevel;
    private String sourceTrace;
    private Integer evidenceCount;
    private String rectificationStatus;
    private Integer sortOrder;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getAnalysisId() {
        return analysisId;
    }

    public void setAnalysisId(Long analysisId) {
        this.analysisId = analysisId;
    }

    public String getIndicatorCode() {
        return indicatorCode;
    }

    public void setIndicatorCode(String indicatorCode) {
        this.indicatorCode = indicatorCode;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getSuggestion() {
        return suggestion;
    }

    public void setSuggestion(String suggestion) {
        this.suggestion = suggestion;
    }

    public String getRelatedClause() {
        return relatedClause;
    }

    public void setRelatedClause(String relatedClause) {
        this.relatedClause = relatedClause;
    }

    public String getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(String riskLevel) {
        this.riskLevel = riskLevel;
    }

    public String getSourceTrace() {
        return sourceTrace;
    }

    public void setSourceTrace(String sourceTrace) {
        this.sourceTrace = sourceTrace;
    }

    public Integer getEvidenceCount() {
        return evidenceCount;
    }

    public void setEvidenceCount(Integer evidenceCount) {
        this.evidenceCount = evidenceCount;
    }

    public String getRectificationStatus() {
        return rectificationStatus;
    }

    public void setRectificationStatus(String rectificationStatus) {
        this.rectificationStatus = rectificationStatus;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }
}
