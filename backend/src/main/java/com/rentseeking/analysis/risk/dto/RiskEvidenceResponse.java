package com.rentseeking.analysis.risk.dto;

import java.math.BigDecimal;

public class RiskEvidenceResponse {

    private Long id;
    private Long analysisId;
    private Long riskItemId;
    private String evidenceType;
    private String sourceType;
    private Long sourceId;
    private String title;
    private String snippet;
    private BigDecimal similarity;
    private Integer citationNo;

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

    public Long getRiskItemId() {
        return riskItemId;
    }

    public void setRiskItemId(Long riskItemId) {
        this.riskItemId = riskItemId;
    }

    public String getEvidenceType() {
        return evidenceType;
    }

    public void setEvidenceType(String evidenceType) {
        this.evidenceType = evidenceType;
    }

    public String getSourceType() {
        return sourceType;
    }

    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }

    public Long getSourceId() {
        return sourceId;
    }

    public void setSourceId(Long sourceId) {
        this.sourceId = sourceId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSnippet() {
        return snippet;
    }

    public void setSnippet(String snippet) {
        this.snippet = snippet;
    }

    public BigDecimal getSimilarity() {
        return similarity;
    }

    public void setSimilarity(BigDecimal similarity) {
        this.similarity = similarity;
    }

    public Integer getCitationNo() {
        return citationNo;
    }

    public void setCitationNo(Integer citationNo) {
        this.citationNo = citationNo;
    }
}
