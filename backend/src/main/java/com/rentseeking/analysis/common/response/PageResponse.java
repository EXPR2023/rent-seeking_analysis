package com.rentseeking.analysis.common.response;

import java.util.List;

public class PageResponse<T> {

    private List<T> records;
    private Integer pageNo;
    private Integer pageSize;
    private Long total;

    public PageResponse() {
    }

    public PageResponse(List<T> records, Integer pageNo, Integer pageSize, Long total) {
        this.records = records;
        this.pageNo = pageNo;
        this.pageSize = pageSize;
        this.total = total;
    }

    public List<T> getRecords() {
        return records;
    }

    public void setRecords(List<T> records) {
        this.records = records;
    }

    public Integer getPageNo() {
        return pageNo;
    }

    public void setPageNo(Integer pageNo) {
        this.pageNo = pageNo;
    }

    public Integer getPageSize() {
        return pageSize;
    }

    public void setPageSize(Integer pageSize) {
        this.pageSize = pageSize;
    }

    public Long getTotal() {
        return total;
    }

    public void setTotal(Long total) {
        this.total = total;
    }
}
