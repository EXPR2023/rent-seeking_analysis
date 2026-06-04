package com.rentseeking.analysis.regulation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public class RegulationSaveRequest {

    private Long regulationSetId;

    @NotBlank
    @Size(max = 255)
    private String title;

    @Size(max = 128)
    private String code;

    @NotBlank
    @Size(max = 64)
    private String typeCode;

    @Size(max = 128)
    private String publishDepartment;

    private LocalDate effectiveDate;
    private LocalDate expiryDate;

    @Size(max = 500)
    private String applicableScope;

    @NotBlank
    @Size(max = 32)
    private String status;

    private String content;

    public Long getRegulationSetId() {
        return regulationSetId;
    }

    public void setRegulationSetId(Long regulationSetId) {
        this.regulationSetId = regulationSetId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getTypeCode() {
        return typeCode;
    }

    public void setTypeCode(String typeCode) {
        this.typeCode = typeCode;
    }

    public String getPublishDepartment() {
        return publishDepartment;
    }

    public void setPublishDepartment(String publishDepartment) {
        this.publishDepartment = publishDepartment;
    }

    public LocalDate getEffectiveDate() {
        return effectiveDate;
    }

    public void setEffectiveDate(LocalDate effectiveDate) {
        this.effectiveDate = effectiveDate;
    }

    public LocalDate getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(LocalDate expiryDate) {
        this.expiryDate = expiryDate;
    }

    public String getApplicableScope() {
        return applicableScope;
    }

    public void setApplicableScope(String applicableScope) {
        this.applicableScope = applicableScope;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
