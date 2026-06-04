package com.rentseeking.analysis.conflict.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.ArrayList;
import java.util.List;

public class ConflictTaskCreateRequest {

    @NotNull
    private Long mainRegulationId;

    @NotEmpty
    private List<Long> compareRegulationIds = new ArrayList<>();

    public Long getMainRegulationId() {
        return mainRegulationId;
    }

    public void setMainRegulationId(Long mainRegulationId) {
        this.mainRegulationId = mainRegulationId;
    }

    public List<Long> getCompareRegulationIds() {
        return compareRegulationIds;
    }

    public void setCompareRegulationIds(List<Long> compareRegulationIds) {
        this.compareRegulationIds = compareRegulationIds;
    }
}
