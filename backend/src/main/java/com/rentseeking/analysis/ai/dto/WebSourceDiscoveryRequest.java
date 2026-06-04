package com.rentseeking.analysis.ai.dto;

import jakarta.validation.constraints.Size;

import java.util.ArrayList;
import java.util.List;

public class WebSourceDiscoveryRequest {

    @Size(max = 200)
    private String caseTitle;

    @Size(max = 5000)
    private String caseDescription;

    @Size(max = 300)
    private String query;

    private Long regulationSetId;

    private List<Long> relatedRegulationIds = new ArrayList<>();

    private List<String> sourceUrls = new ArrayList<>();

    private Integer maxResults = 6;

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

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public Long getRegulationSetId() {
        return regulationSetId;
    }

    public void setRegulationSetId(Long regulationSetId) {
        this.regulationSetId = regulationSetId;
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

    public Integer getMaxResults() {
        return maxResults;
    }

    public void setMaxResults(Integer maxResults) {
        this.maxResults = maxResults;
    }
}
