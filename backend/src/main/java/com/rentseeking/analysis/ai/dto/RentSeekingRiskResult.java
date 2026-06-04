package com.rentseeking.analysis.ai.dto;

import java.util.ArrayList;
import java.util.List;

public class RentSeekingRiskResult {

    private String summary;
    private String riskLevel;
    private Integer riskScore;
    private String overallSuggestion;
    private String rawResponse;
    private List<RentSeekingRiskItem> riskItems = new ArrayList<>();

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(String riskLevel) {
        this.riskLevel = riskLevel;
    }

    public Integer getRiskScore() {
        return riskScore;
    }

    public void setRiskScore(Integer riskScore) {
        this.riskScore = riskScore;
    }

    public String getOverallSuggestion() {
        return overallSuggestion;
    }

    public void setOverallSuggestion(String overallSuggestion) {
        this.overallSuggestion = overallSuggestion;
    }

    public String getRawResponse() {
        return rawResponse;
    }

    public void setRawResponse(String rawResponse) {
        this.rawResponse = rawResponse;
    }

    public List<RentSeekingRiskItem> getRiskItems() {
        return riskItems;
    }

    public void setRiskItems(List<RentSeekingRiskItem> riskItems) {
        this.riskItems = riskItems;
    }
}
