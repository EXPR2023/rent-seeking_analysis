package com.rentseeking.analysis.regulation.dto;

import java.time.LocalDateTime;

public class RegulationSetResponse {

    private Long id;
    private String setName;
    private String setCode;
    private String description;
    private Boolean enabled;
    private Boolean defaultSet;
    private Integer regulationCount;
    private String createdByName;
    private LocalDateTime createdAt;
    private String updatedByName;
    private LocalDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSetName() {
        return setName;
    }

    public void setSetName(String setName) {
        this.setName = setName;
    }

    public String getSetCode() {
        return setCode;
    }

    public void setSetCode(String setCode) {
        this.setCode = setCode;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public Boolean getDefaultSet() {
        return defaultSet;
    }

    public void setDefaultSet(Boolean defaultSet) {
        this.defaultSet = defaultSet;
    }

    public Integer getRegulationCount() {
        return regulationCount;
    }

    public void setRegulationCount(Integer regulationCount) {
        this.regulationCount = regulationCount;
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

    public String getUpdatedByName() {
        return updatedByName;
    }

    public void setUpdatedByName(String updatedByName) {
        this.updatedByName = updatedByName;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
