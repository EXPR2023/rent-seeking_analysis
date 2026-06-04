package com.rentseeking.analysis.common.response;

import com.rentseeking.analysis.common.constant.ErrorCode;
import com.rentseeking.analysis.common.util.TraceIdUtils;

public class ApiResponse<T> {

    private String code;
    private String message;
    private T data;
    private String traceId;

    public ApiResponse() {
    }

    public ApiResponse(String code, String message, T data, String traceId) {
        this.code = code;
        this.message = message;
        this.data = data;
        this.traceId = traceId;
    }

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(ErrorCode.SUCCESS, "操作成功", data, TraceIdUtils.currentTraceId());
    }

    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(ErrorCode.SUCCESS, message, data, TraceIdUtils.currentTraceId());
    }

    public static <T> ApiResponse<T> fail(String code, String message) {
        return new ApiResponse<>(code, message, null, TraceIdUtils.currentTraceId());
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }
}
