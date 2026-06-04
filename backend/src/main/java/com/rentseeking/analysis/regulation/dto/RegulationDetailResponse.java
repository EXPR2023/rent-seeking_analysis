package com.rentseeking.analysis.regulation.dto;

public class RegulationDetailResponse extends RegulationResponse {

    private String content;
    private String plainText;

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getPlainText() {
        return plainText;
    }

    public void setPlainText(String plainText) {
        this.plainText = plainText;
    }
}
