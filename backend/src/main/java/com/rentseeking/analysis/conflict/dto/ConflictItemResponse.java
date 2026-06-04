package com.rentseeking.analysis.conflict.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class ConflictItemResponse {

    private Long id;
    private Long taskId;
    private Long mainRegulationId;
    private Long compareRegulationId;
    private String compareRegulationTitle;
    private String conflictType;
    private String conflictLevel;
    private String mainClause;
    private String compareClause;
    private BigDecimal similarity;
    private String explanation;
    private String status;
    private String reviewComment;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getTaskId() {
        return taskId;
    }

    public void setTaskId(Long taskId) {
        this.taskId = taskId;
    }

    public Long getMainRegulationId() {
        return mainRegulationId;
    }

    public void setMainRegulationId(Long mainRegulationId) {
        this.mainRegulationId = mainRegulationId;
    }

    public Long getCompareRegulationId() {
        return compareRegulationId;
    }

    public void setCompareRegulationId(Long compareRegulationId) {
        this.compareRegulationId = compareRegulationId;
    }

    public String getCompareRegulationTitle() {
        return compareRegulationTitle;
    }

    public void setCompareRegulationTitle(String compareRegulationTitle) {
        this.compareRegulationTitle = compareRegulationTitle;
    }

    public String getConflictType() {
        return conflictType;
    }

    public void setConflictType(String conflictType) {
        this.conflictType = conflictType;
    }

    public String getConflictLevel() {
        return conflictLevel;
    }

    public void setConflictLevel(String conflictLevel) {
        this.conflictLevel = conflictLevel;
    }

    public String getMainClause() {
        return mainClause;
    }

    public void setMainClause(String mainClause) {
        this.mainClause = mainClause;
    }

    public String getCompareClause() {
        return compareClause;
    }

    public void setCompareClause(String compareClause) {
        this.compareClause = compareClause;
    }

    public BigDecimal getSimilarity() {
        return similarity;
    }

    public void setSimilarity(BigDecimal similarity) {
        this.similarity = similarity;
    }

    public String getExplanation() {
        return explanation;
    }

    public void setExplanation(String explanation) {
        this.explanation = explanation;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getReviewComment() {
        return reviewComment;
    }

    public void setReviewComment(String reviewComment) {
        this.reviewComment = reviewComment;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
