package com.rentseeking.analysis.conflict.dto;

import jakarta.validation.constraints.Size;

public class ConflictItemReviewRequest {

    @Size(max = 500)
    private String reviewComment;

    public String getReviewComment() {
        return reviewComment;
    }

    public void setReviewComment(String reviewComment) {
        this.reviewComment = reviewComment;
    }
}
