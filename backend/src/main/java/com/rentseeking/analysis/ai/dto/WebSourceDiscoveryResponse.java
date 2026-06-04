package com.rentseeking.analysis.ai.dto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class WebSourceDiscoveryResponse {

    private String query;
    private List<String> searchedQueries = new ArrayList<>();
    private List<WebSourceCandidate> candidates = new ArrayList<>();
    private LocalDateTime discoveredAt;

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public List<String> getSearchedQueries() {
        return searchedQueries;
    }

    public void setSearchedQueries(List<String> searchedQueries) {
        this.searchedQueries = searchedQueries == null ? new ArrayList<>() : searchedQueries;
    }

    public List<WebSourceCandidate> getCandidates() {
        return candidates;
    }

    public void setCandidates(List<WebSourceCandidate> candidates) {
        this.candidates = candidates == null ? new ArrayList<>() : candidates;
    }

    public LocalDateTime getDiscoveredAt() {
        return discoveredAt;
    }

    public void setDiscoveredAt(LocalDateTime discoveredAt) {
        this.discoveredAt = discoveredAt;
    }

    public static class WebSourceCandidate {
        private String url;
        private String title;
        private String domain;
        private String sourceType;
        private Boolean officialSource;
        private String fetchStatus;
        private String message;
        private String snippet;
        private Integer contentLength;
        private String suggestedTypeCode;
        private String suggestedPublishDepartment;

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

        public String getDomain() {
            return domain;
        }

        public void setDomain(String domain) {
            this.domain = domain;
        }

        public String getSourceType() {
            return sourceType;
        }

        public void setSourceType(String sourceType) {
            this.sourceType = sourceType;
        }

        public Boolean getOfficialSource() {
            return officialSource;
        }

        public void setOfficialSource(Boolean officialSource) {
            this.officialSource = officialSource;
        }

        public String getFetchStatus() {
            return fetchStatus;
        }

        public void setFetchStatus(String fetchStatus) {
            this.fetchStatus = fetchStatus;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public String getSnippet() {
            return snippet;
        }

        public void setSnippet(String snippet) {
            this.snippet = snippet;
        }

        public Integer getContentLength() {
            return contentLength;
        }

        public void setContentLength(Integer contentLength) {
            this.contentLength = contentLength;
        }

        public String getSuggestedTypeCode() {
            return suggestedTypeCode;
        }

        public void setSuggestedTypeCode(String suggestedTypeCode) {
            this.suggestedTypeCode = suggestedTypeCode;
        }

        public String getSuggestedPublishDepartment() {
            return suggestedPublishDepartment;
        }

        public void setSuggestedPublishDepartment(String suggestedPublishDepartment) {
            this.suggestedPublishDepartment = suggestedPublishDepartment;
        }
    }
}
