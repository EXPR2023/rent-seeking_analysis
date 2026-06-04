package com.rentseeking.analysis.system.dto;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

public class HealthResponse {

    private String backend;
    private String mysql;
    private String redis;
    private String ai;
    private LocalDateTime checkedAt;
    private Map<String, String> details = new LinkedHashMap<>();

    public String getBackend() {
        return backend;
    }

    public void setBackend(String backend) {
        this.backend = backend;
    }

    public String getMysql() {
        return mysql;
    }

    public void setMysql(String mysql) {
        this.mysql = mysql;
    }

    public String getRedis() {
        return redis;
    }

    public void setRedis(String redis) {
        this.redis = redis;
    }

    public String getAi() {
        return ai;
    }

    public void setAi(String ai) {
        this.ai = ai;
    }

    public LocalDateTime getCheckedAt() {
        return checkedAt;
    }

    public void setCheckedAt(LocalDateTime checkedAt) {
        this.checkedAt = checkedAt;
    }

    public Map<String, String> getDetails() {
        return details;
    }

    public void setDetails(Map<String, String> details) {
        this.details = details;
    }
}
