package com.rentseeking.analysis.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.ArrayList;
import java.util.List;

public class CaseAnalysisRequest {

    @Size(max = 64)
    private String analysisType = "CASE_ANALYSIS";

    @NotBlank
    @Size(max = 200)
    private String caseTitle;

    @NotBlank
    @Size(max = 5000)
    private String caseDescription;

    private Long regulationSetId;

    private Long externalRegulationSetId;

    private Boolean jointAnalysis = true;

    private Boolean externalDiscovery = true;

    private List<Long> relatedRegulationIds = new ArrayList<>();

    private List<String> sourceUrls = new ArrayList<>();

    public String getAnalysisType() {
        return analysisType;
    }

    public void setAnalysisType(String analysisType) {
        this.analysisType = analysisType == null || analysisType.isBlank() ? "CASE_ANALYSIS" : analysisType.trim();
    }

    public String getCaseTitle() {
        return caseTitle;
    }

    public void setCaseTitle(String caseTitle) {
        this.caseTitle = caseTitle;
    }

    public String getCaseDescription() {
        return caseDescription;
    }

    public void setCaseDescription(String caseDescription) {
        this.caseDescription = caseDescription;
    }

    public Long getRegulationSetId() {
        return regulationSetId;
    }

    public void setRegulationSetId(Long regulationSetId) {
        this.regulationSetId = regulationSetId;
    }

    public Long getExternalRegulationSetId() {
        return externalRegulationSetId;
    }

    public void setExternalRegulationSetId(Long externalRegulationSetId) {
        this.externalRegulationSetId = externalRegulationSetId;
    }

    public Boolean getJointAnalysis() {
        return jointAnalysis;
    }

    public void setJointAnalysis(Boolean jointAnalysis) {
        this.jointAnalysis = jointAnalysis;
    }

    public Boolean getExternalDiscovery() {
        return externalDiscovery;
    }

    public void setExternalDiscovery(Boolean externalDiscovery) {
        this.externalDiscovery = externalDiscovery;
    }

    public List<Long> getRelatedRegulationIds() {
        return relatedRegulationIds;
    }

    public void setRelatedRegulationIds(List<Long> relatedRegulationIds) {
        this.relatedRegulationIds = relatedRegulationIds == null ? new ArrayList<>() : relatedRegulationIds;
    }

    public List<String> getSourceUrls() {
        return sourceUrls;
    }

    public void setSourceUrls(List<String> sourceUrls) {
        this.sourceUrls = sourceUrls == null ? new ArrayList<>() : sourceUrls;
    }
}
