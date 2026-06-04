package com.rentseeking.analysis.ai.dto;

public class RentSeekingRiskItem {

    private String indicatorCode;
    private String title;
    private String description;
    private String reason;
    private String suggestion;
    private String relatedClause;
    private String riskLevel;

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
}
