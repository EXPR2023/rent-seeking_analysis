package com.rentseeking.analysis.regulation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class RegulationSetRequest {

    @NotBlank
    @Size(max = 128)
    private String setName;

    @NotBlank
    @Size(max = 64)
    private String setCode;

    @Size(max = 500)
    private String description;

    private Boolean enabled = true;

    private Boolean defaultSet = false;

    public String getSetName() {
        return setName;
    }

    public void setSetName(String setName) {
        this.setName = setName;
    }

    public String getSetCode() {
        return setCode;
    }

    public void setSetCode(String setCode) {
        this.setCode = setCode;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public Boolean getDefaultSet() {
        return defaultSet;
    }

    public void setDefaultSet(Boolean defaultSet) {
        this.defaultSet = defaultSet;
    }
}
