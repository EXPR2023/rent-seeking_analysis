package com.rentseeking.analysis.system.dto;

import jakarta.validation.constraints.Size;

public class UpdateSystemConfigRequest {

    @Size(max = 1000)
    private String configValue;

    public String getConfigValue() {
        return configValue;
    }

    public void setConfigValue(String configValue) {
        this.configValue = configValue;
    }
}
