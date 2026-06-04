package com.rentseeking.analysis.ai.dto;

public class RegulationQaResponse {

    private Long regulationId;
    private String question;
    private String answer;
    private Long recordId;
    private Boolean fallback;

    public Long getRegulationId() {
        return regulationId;
    }

    public void setRegulationId(Long regulationId) {
        this.regulationId = regulationId;
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
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
}
