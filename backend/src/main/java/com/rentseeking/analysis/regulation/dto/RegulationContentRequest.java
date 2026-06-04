package com.rentseeking.analysis.regulation.dto;

import jakarta.validation.constraints.NotNull;

public class RegulationContentRequest {

    @NotNull
    private String content;

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
