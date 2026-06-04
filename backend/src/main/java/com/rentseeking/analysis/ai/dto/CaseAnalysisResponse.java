package com.rentseeking.analysis.ai.dto;

import java.util.ArrayList;
import java.util.List;

public class CaseAnalysisResponse {

    private String caseTitle;
    private String summary;
    private String riskLevel;
    private Integer riskScore;
    private String analysisMode;
    private Long recordId;
    private Boolean fallback;
    private Boolean jointAnalysis;
    private Long primaryRegulationSetId;
    private String primaryRegulationSetName;
    private String jointSummary;
    private List<RelatedDocument> relatedDocuments = new ArrayList<>();
    private List<OrganizationNode> organizationNodes = new ArrayList<>();
    private List<CaseRiskItem> riskItems = new ArrayList<>();
    private List<InternalRisk> internalRisks = new ArrayList<>();
    private List<ControlMeasure> controlMeasures = new ArrayList<>();
    private List<CaseRegulationMapping> caseRegulationMappings = new ArrayList<>();
    private List<ExternalConstraint> externalConstraints = new ArrayList<>();
    private List<ClosureGap> closureGaps = new ArrayList<>();
    private List<IterationTrace> iterationTraces = new ArrayList<>();
    private List<ManualImportSuggestion> manualImportSuggestions = new ArrayList<>();
    private List<FetchedSource> fetchedSources = new ArrayList<>();
    private List<String> evidenceSnippets = new ArrayList<>();

    public String getCaseTitle() {
        return caseTitle;
    }

    public void setCaseTitle(String caseTitle) {
        this.caseTitle = caseTitle;
    }

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

    public String getAnalysisMode() {
        return analysisMode;
    }

    public void setAnalysisMode(String analysisMode) {
        this.analysisMode = analysisMode;
    }

    public Long getRecordId() {
        return recordId;
    }

    public void setRecordId(Long recordId) {
        this.recordId = recordId;
    }

    public Boolean getFallback() {
        return fallback;
    }

    public void setFallback(Boolean fallback) {
        this.fallback = fallback;
    }

    public Boolean getJointAnalysis() {
        return jointAnalysis;
    }

    public void setJointAnalysis(Boolean jointAnalysis) {
        this.jointAnalysis = jointAnalysis;
    }

    public Long getPrimaryRegulationSetId() {
        return primaryRegulationSetId;
    }

    public void setPrimaryRegulationSetId(Long primaryRegulationSetId) {
        this.primaryRegulationSetId = primaryRegulationSetId;
    }

    public String getPrimaryRegulationSetName() {
        return primaryRegulationSetName;
    }

    public void setPrimaryRegulationSetName(String primaryRegulationSetName) {
        this.primaryRegulationSetName = primaryRegulationSetName;
    }

    public String getJointSummary() {
        return jointSummary;
    }

    public void setJointSummary(String jointSummary) {
        this.jointSummary = jointSummary;
    }

    public List<RelatedDocument> getRelatedDocuments() {
        return relatedDocuments;
    }

    public void setRelatedDocuments(List<RelatedDocument> relatedDocuments) {
        this.relatedDocuments = relatedDocuments == null ? new ArrayList<>() : relatedDocuments;
    }

    public List<OrganizationNode> getOrganizationNodes() {
        return organizationNodes;
    }

    public void setOrganizationNodes(List<OrganizationNode> organizationNodes) {
        this.organizationNodes = organizationNodes == null ? new ArrayList<>() : organizationNodes;
    }

    public List<CaseRiskItem> getRiskItems() {
        return riskItems;
    }

    public void setRiskItems(List<CaseRiskItem> riskItems) {
        this.riskItems = riskItems == null ? new ArrayList<>() : riskItems;
    }

    public List<InternalRisk> getInternalRisks() {
        return internalRisks;
    }

    public void setInternalRisks(List<InternalRisk> internalRisks) {
        this.internalRisks = internalRisks == null ? new ArrayList<>() : internalRisks;
    }

    public List<ControlMeasure> getControlMeasures() {
        return controlMeasures;
    }

    public void setControlMeasures(List<ControlMeasure> controlMeasures) {
        this.controlMeasures = controlMeasures == null ? new ArrayList<>() : controlMeasures;
    }

    public List<CaseRegulationMapping> getCaseRegulationMappings() {
        return caseRegulationMappings;
    }

    public void setCaseRegulationMappings(List<CaseRegulationMapping> caseRegulationMappings) {
        this.caseRegulationMappings = caseRegulationMappings == null ? new ArrayList<>() : caseRegulationMappings;
    }

    public List<ExternalConstraint> getExternalConstraints() {
        return externalConstraints;
    }

    public void setExternalConstraints(List<ExternalConstraint> externalConstraints) {
        this.externalConstraints = externalConstraints == null ? new ArrayList<>() : externalConstraints;
    }

    public List<ClosureGap> getClosureGaps() {
        return closureGaps;
    }

    public void setClosureGaps(List<ClosureGap> closureGaps) {
        this.closureGaps = closureGaps == null ? new ArrayList<>() : closureGaps;
    }

    public List<IterationTrace> getIterationTraces() {
        return iterationTraces;
    }

    public void setIterationTraces(List<IterationTrace> iterationTraces) {
        this.iterationTraces = iterationTraces == null ? new ArrayList<>() : iterationTraces;
    }

    public List<ManualImportSuggestion> getManualImportSuggestions() {
        return manualImportSuggestions;
    }

    public void setManualImportSuggestions(List<ManualImportSuggestion> manualImportSuggestions) {
        this.manualImportSuggestions = manualImportSuggestions == null ? new ArrayList<>() : manualImportSuggestions;
    }

    public List<FetchedSource> getFetchedSources() {
        return fetchedSources;
    }

    public void setFetchedSources(List<FetchedSource> fetchedSources) {
        this.fetchedSources = fetchedSources == null ? new ArrayList<>() : fetchedSources;
    }

    public List<String> getEvidenceSnippets() {
        return evidenceSnippets;
    }

    public void setEvidenceSnippets(List<String> evidenceSnippets) {
        this.evidenceSnippets = evidenceSnippets == null ? new ArrayList<>() : evidenceSnippets;
    }

    public static class RelatedDocument {
        private String title;
        private String issuingBody;
        private String sourceUrl;
        private String relevance;
        private String importStatus;

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getIssuingBody() {
            return issuingBody;
        }

        public void setIssuingBody(String issuingBody) {
            this.issuingBody = issuingBody;
        }

        public String getSourceUrl() {
            return sourceUrl;
        }

        public void setSourceUrl(String sourceUrl) {
            this.sourceUrl = sourceUrl;
        }

        public String getRelevance() {
            return relevance;
        }

        public void setRelevance(String relevance) {
            this.relevance = relevance;
        }

        public String getImportStatus() {
            return importStatus;
        }

        public void setImportStatus(String importStatus) {
            this.importStatus = importStatus;
        }
    }

    public static class OrganizationNode {
        private String organization;
        private String level;
        private String role;
        private String riskResponsibility;
        private String evidenceNeed;

        public String getOrganization() {
            return organization;
        }

        public void setOrganization(String organization) {
            this.organization = organization;
        }

        public String getLevel() {
            return level;
        }

        public void setLevel(String level) {
            this.level = level;
        }

        public String getRole() {
            return role;
        }

        public void setRole(String role) {
            this.role = role;
        }

        public String getRiskResponsibility() {
            return riskResponsibility;
        }

        public void setRiskResponsibility(String riskResponsibility) {
            this.riskResponsibility = riskResponsibility;
        }

        public String getEvidenceNeed() {
            return evidenceNeed;
        }

        public void setEvidenceNeed(String evidenceNeed) {
            this.evidenceNeed = evidenceNeed;
        }
    }

    public static class CaseRiskItem {
        private String indicatorCode;
        private String title;
        private String mechanism;
        private String reason;
        private String evidence;
        private String suggestion;
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

        public String getMechanism() {
            return mechanism;
        }

        public void setMechanism(String mechanism) {
            this.mechanism = mechanism;
        }

        public String getReason() {
            return reason;
        }

        public void setReason(String reason) {
            this.reason = reason;
        }

        public String getEvidence() {
            return evidence;
        }

        public void setEvidence(String evidence) {
            this.evidence = evidence;
        }

        public String getSuggestion() {
            return suggestion;
        }

        public void setSuggestion(String suggestion) {
            this.suggestion = suggestion;
        }

        public String getRiskLevel() {
            return riskLevel;
        }

        public void setRiskLevel(String riskLevel) {
            this.riskLevel = riskLevel;
        }
    }

    public static class InternalRisk {
        private String source;
        private String riskType;
        private String riskTitle;
        private String mechanism;
        private String relatedRegulations;
        private String riskLevel;

        public String getSource() {
            return source;
        }

        public void setSource(String source) {
            this.source = source;
        }

        public String getRiskType() {
            return riskType;
        }

        public void setRiskType(String riskType) {
            this.riskType = riskType;
        }

        public String getRiskTitle() {
            return riskTitle;
        }

        public void setRiskTitle(String riskTitle) {
            this.riskTitle = riskTitle;
        }

        public String getMechanism() {
            return mechanism;
        }

        public void setMechanism(String mechanism) {
            this.mechanism = mechanism;
        }

        public String getRelatedRegulations() {
            return relatedRegulations;
        }

        public void setRelatedRegulations(String relatedRegulations) {
            this.relatedRegulations = relatedRegulations;
        }

        public String getRiskLevel() {
            return riskLevel;
        }

        public void setRiskLevel(String riskLevel) {
            this.riskLevel = riskLevel;
        }
    }

    public static class ControlMeasure {
        private String riskTitle;
        private String controlObjective;
        private String measure;
        private String evidenceNeed;
        private String externalSearchHint;
        private String priority;

        public String getRiskTitle() {
            return riskTitle;
        }

        public void setRiskTitle(String riskTitle) {
            this.riskTitle = riskTitle;
        }

        public String getControlObjective() {
            return controlObjective;
        }

        public void setControlObjective(String controlObjective) {
            this.controlObjective = controlObjective;
        }

        public String getMeasure() {
            return measure;
        }

        public void setMeasure(String measure) {
            this.measure = measure;
        }

        public String getEvidenceNeed() {
            return evidenceNeed;
        }

        public void setEvidenceNeed(String evidenceNeed) {
            this.evidenceNeed = evidenceNeed;
        }

        public String getExternalSearchHint() {
            return externalSearchHint;
        }

        public void setExternalSearchHint(String externalSearchHint) {
            this.externalSearchHint = externalSearchHint;
        }

        public String getPriority() {
            return priority;
        }

        public void setPriority(String priority) {
            this.priority = priority;
        }
    }

    public static class CaseRegulationMapping {
        private String caseFact;
        private String matchedRegulation;
        private String matchedRisk;
        private String gapType;
        private String explanation;

        public String getCaseFact() {
            return caseFact;
        }

        public void setCaseFact(String caseFact) {
            this.caseFact = caseFact;
        }

        public String getMatchedRegulation() {
            return matchedRegulation;
        }

        public void setMatchedRegulation(String matchedRegulation) {
            this.matchedRegulation = matchedRegulation;
        }

        public String getMatchedRisk() {
            return matchedRisk;
        }

        public void setMatchedRisk(String matchedRisk) {
            this.matchedRisk = matchedRisk;
        }

        public String getGapType() {
            return gapType;
        }

        public void setGapType(String gapType) {
            this.gapType = gapType;
        }

        public String getExplanation() {
            return explanation;
        }

        public void setExplanation(String explanation) {
            this.explanation = explanation;
        }
    }

    public static class ExternalConstraint {
        private String sourceSet;
        private String documentTitle;
        private String constraintType;
        private String constrainsRisk;
        private String constraintMechanism;
        private String sufficiency;
        private String importAdvice;

        public String getSourceSet() {
            return sourceSet;
        }

        public void setSourceSet(String sourceSet) {
            this.sourceSet = sourceSet;
        }

        public String getDocumentTitle() {
            return documentTitle;
        }

        public void setDocumentTitle(String documentTitle) {
            this.documentTitle = documentTitle;
        }

        public String getConstraintType() {
            return constraintType;
        }

        public void setConstraintType(String constraintType) {
            this.constraintType = constraintType;
        }

        public String getConstrainsRisk() {
            return constrainsRisk;
        }

        public void setConstrainsRisk(String constrainsRisk) {
            this.constrainsRisk = constrainsRisk;
        }

        public String getConstraintMechanism() {
            return constraintMechanism;
        }

        public void setConstraintMechanism(String constraintMechanism) {
            this.constraintMechanism = constraintMechanism;
        }

        public String getSufficiency() {
            return sufficiency;
        }

        public void setSufficiency(String sufficiency) {
            this.sufficiency = sufficiency;
        }

        public String getImportAdvice() {
            return importAdvice;
        }

        public void setImportAdvice(String importAdvice) {
            this.importAdvice = importAdvice;
        }
    }

    public static class ClosureGap {
        private String gapTitle;
        private String missingLink;
        private String remainingRisk;
        private String recommendedAction;
        private String priority;

        public String getGapTitle() {
            return gapTitle;
        }

        public void setGapTitle(String gapTitle) {
            this.gapTitle = gapTitle;
        }

        public String getMissingLink() {
            return missingLink;
        }

        public void setMissingLink(String missingLink) {
            this.missingLink = missingLink;
        }

        public String getRemainingRisk() {
            return remainingRisk;
        }

        public void setRemainingRisk(String remainingRisk) {
            this.remainingRisk = remainingRisk;
        }

        public String getRecommendedAction() {
            return recommendedAction;
        }

        public void setRecommendedAction(String recommendedAction) {
            this.recommendedAction = recommendedAction;
        }

        public String getPriority() {
            return priority;
        }

        public void setPriority(String priority) {
            this.priority = priority;
        }
    }

    public static class IterationTrace {
        private Integer round;
        private String objective;
        private String query;
        private Integer evidenceCount;
        private String stopReason;

        public Integer getRound() {
            return round;
        }

        public void setRound(Integer round) {
            this.round = round;
        }

        public String getObjective() {
            return objective;
        }

        public void setObjective(String objective) {
            this.objective = objective;
        }

        public String getQuery() {
            return query;
        }

        public void setQuery(String query) {
            this.query = query;
        }

        public Integer getEvidenceCount() {
            return evidenceCount;
        }

        public void setEvidenceCount(Integer evidenceCount) {
            this.evidenceCount = evidenceCount;
        }

        public String getStopReason() {
            return stopReason;
        }

        public void setStopReason(String stopReason) {
            this.stopReason = stopReason;
        }
    }

    public static class ManualImportSuggestion {
        private String category;
        private String title;
        private String suggestedSource;
        private String reason;

        public String getCategory() {
            return category;
        }

        public void setCategory(String category) {
            this.category = category;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getSuggestedSource() {
            return suggestedSource;
        }

        public void setSuggestedSource(String suggestedSource) {
            this.suggestedSource = suggestedSource;
        }

        public String getReason() {
            return reason;
        }

        public void setReason(String reason) {
            this.reason = reason;
        }
    }

    public static class FetchedSource {
        private String url;
        private String title;
        private String status;
        private String message;
        private String snippet;

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public String getSnippet() {
            return snippet;
        }

        public void setSnippet(String snippet) {
            this.snippet = snippet;
        }
    }
}
