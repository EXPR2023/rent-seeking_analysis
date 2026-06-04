package com.rentseeking.analysis.dashboard.dto;

public class DashboardSummaryResponse {

    private Long regulationTotal;
    private Long analyzedRegulationTotal;
    private Long conflictTotal;
    private Long pendingConflictTotal;
    private Long highRiskTotal;
    private Long suggestionTotal;

    public Long getRegulationTotal() {
        return regulationTotal;
    }

    public void setRegulationTotal(Long regulationTotal) {
        this.regulationTotal = regulationTotal;
    }

    public Long getAnalyzedRegulationTotal() {
        return analyzedRegulationTotal;
    }

    public void setAnalyzedRegulationTotal(Long analyzedRegulationTotal) {
        this.analyzedRegulationTotal = analyzedRegulationTotal;
    }

    public Long getConflictTotal() {
        return conflictTotal;
    }

    public void setConflictTotal(Long conflictTotal) {
        this.conflictTotal = conflictTotal;
    }

    public Long getPendingConflictTotal() {
        return pendingConflictTotal;
    }

    public void setPendingConflictTotal(Long pendingConflictTotal) {
        this.pendingConflictTotal = pendingConflictTotal;
    }

    public Long getHighRiskTotal() {
        return highRiskTotal;
    }

    public void setHighRiskTotal(Long highRiskTotal) {
        this.highRiskTotal = highRiskTotal;
    }

    public Long getSuggestionTotal() {
        return suggestionTotal;
    }

    public void setSuggestionTotal(Long suggestionTotal) {
        this.suggestionTotal = suggestionTotal;
    }
}
