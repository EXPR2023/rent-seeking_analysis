package com.rentseeking.analysis.risk.dto;

import java.util.ArrayList;
import java.util.List;

public class RiskAnalysisDetailResponse extends RiskAnalysisResponse {

    private List<RiskItemResponse> items = new ArrayList<>();
    private List<RiskEvidenceResponse> evidences = new ArrayList<>();

    public List<RiskItemResponse> getItems() {
        return items;
    }

    public void setItems(List<RiskItemResponse> items) {
        this.items = items;
    }

    public List<RiskEvidenceResponse> getEvidences() {
        return evidences;
    }

    public void setEvidences(List<RiskEvidenceResponse> evidences) {
        this.evidences = evidences;
    }
}
