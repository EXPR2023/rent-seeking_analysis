package com.rentseeking.analysis.conflict.dto;

import java.time.LocalDateTime;

public class ConflictTaskResponse {

    private Long id;
    private Long mainRegulationId;
    private String mainRegulationTitle;
    private String compareRegulationIds;
    private String status;
    private Integer totalItems;
    private String createdByName;
    private LocalDateTime createdAt;
    private LocalDateTime finishedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getMainRegulationId() {
        return mainRegulationId;
    }

    public void setMainRegulationId(Long mainRegulationId) {
        this.mainRegulationId = mainRegulationId;
    }

    public String getMainRegulationTitle() {
        return mainRegulationTitle;
    }

    public void setMainRegulationTitle(String mainRegulationTitle) {
        this.mainRegulationTitle = mainRegulationTitle;
    }

    public String getCompareRegulationIds() {
        return compareRegulationIds;
    }

    public void setCompareRegulationIds(String compareRegulationIds) {
        this.compareRegulationIds = compareRegulationIds;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getTotalItems() {
        return totalItems;
    }

    public void setTotalItems(Integer totalItems) {
        this.totalItems = totalItems;
    }

    public String getCreatedByName() {
        return createdByName;
    }

    public void setCreatedByName(String createdByName) {
        this.createdByName = createdByName;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getFinishedAt() {
        return finishedAt;
    }

    public void setFinishedAt(LocalDateTime finishedAt) {
        this.finishedAt = finishedAt;
    }
}
