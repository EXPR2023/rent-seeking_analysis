package com.rentseeking.analysis.dashboard.dto;

public class ChartItemResponse {

    private String name;
    private String code;
    private Long value;

    public ChartItemResponse() {
    }

    public ChartItemResponse(String name, String code, Long value) {
        this.name = name;
        this.code = code;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public Long getValue() {
        return value;
    }

    public void setValue(Long value) {
        this.value = value;
    }
}
