package com.rentseeking.analysis.conflict.dto;

import java.util.ArrayList;
import java.util.List;

public class ConflictTaskDetailResponse extends ConflictTaskResponse {

    private List<ConflictItemResponse> items = new ArrayList<>();

    public List<ConflictItemResponse> getItems() {
        return items;
    }

    public void setItems(List<ConflictItemResponse> items) {
        this.items = items;
    }
}
