package com.rentseeking.analysis.ai.dto;

import jakarta.validation.constraints.Size;

import java.util.ArrayList;
import java.util.List;

public class WebSourceImportRequest {

    private Long regulationSetId;

    @Size(max = 30)
    private List<ImportSource> sources = new ArrayList<>();

    public Long getRegulationSetId() {
        return regulationSetId;
    }

    public void setRegulationSetId(Long regulationSetId) {
        this.regulationSetId = regulationSetId;
    }

    public List<ImportSource> getSources() {
        return sources;
    }

    public void setSources(List<ImportSource> sources) {
        this.sources = sources == null ? new ArrayList<>() : sources;
    }

    public static class ImportSource {
        @Size(max = 1200)
        private String url;

        @Size(max = 255)
        private String title;

        @Size(max = 64)
        private String typeCode;

        @Size(max = 128)
        private String publishDepartment;

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
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
    }
}
