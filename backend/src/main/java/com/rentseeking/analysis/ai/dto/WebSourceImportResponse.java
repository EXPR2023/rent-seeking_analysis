package com.rentseeking.analysis.ai.dto;

import java.util.ArrayList;
import java.util.List;

public class WebSourceImportResponse {

    private Long regulationSetId;
    private String regulationSetName;
    private Integer importedCount;
    private Integer skippedCount;
    private Integer failedCount;
    private List<ImportResult> results = new ArrayList<>();

    public Long getRegulationSetId() {
        return regulationSetId;
    }

    public void setRegulationSetId(Long regulationSetId) {
        this.regulationSetId = regulationSetId;
    }

    public String getRegulationSetName() {
        return regulationSetName;
    }

    public void setRegulationSetName(String regulationSetName) {
        this.regulationSetName = regulationSetName;
    }

    public Integer getImportedCount() {
        return importedCount;
    }

    public void setImportedCount(Integer importedCount) {
        this.importedCount = importedCount;
    }

    public Integer getSkippedCount() {
        return skippedCount;
    }

    public void setSkippedCount(Integer skippedCount) {
        this.skippedCount = skippedCount;
    }

    public Integer getFailedCount() {
        return failedCount;
    }

    public void setFailedCount(Integer failedCount) {
        this.failedCount = failedCount;
    }

    public List<ImportResult> getResults() {
        return results;
    }

    public void setResults(List<ImportResult> results) {
        this.results = results == null ? new ArrayList<>() : results;
    }

    public static class ImportResult {
        private String url;
        private String title;
        private Long regulationId;
        private String status;
        private String message;

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

        public Long getRegulationId() {
            return regulationId;
        }

        public void setRegulationId(Long regulationId) {
            this.regulationId = regulationId;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }
}
