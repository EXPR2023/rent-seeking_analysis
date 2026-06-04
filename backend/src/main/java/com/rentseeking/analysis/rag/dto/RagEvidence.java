package com.rentseeking.analysis.rag.dto;

import java.math.BigDecimal;

public class RagEvidence {

    private Long chunkId;
    private String sourceType;
    private Long sourceId;
    private String title;
    private String snippet;
    private BigDecimal similarity;
    private Integer citationNo;

    public Long getChunkId() {
        return chunkId;
    }

    public void setChunkId(Long chunkId) {
        this.chunkId = chunkId;
    }

    public String getSourceType() {
        return sourceType;
    }

    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }

    public Long getSourceId() {
        return sourceId;
    }

    public void setSourceId(Long sourceId) {
        this.sourceId = sourceId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSnippet() {
        return snippet;
    }

    public void setSnippet(String snippet) {
        this.snippet = snippet;
    }

    public BigDecimal getSimilarity() {
        return similarity;
    }

    public void setSimilarity(BigDecimal similarity) {
        this.similarity = similarity;
    }

    public Integer getCitationNo() {
        return citationNo;
    }

    public void setCitationNo(Integer citationNo) {
        this.citationNo = citationNo;
    }
}
