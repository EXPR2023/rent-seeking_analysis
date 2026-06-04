package com.rentseeking.analysis.common.exception;

import com.rentseeking.analysis.common.constant.ErrorCode;

public class BizException extends RuntimeException {

    private final String code;

    public BizException(String message) {
        this(ErrorCode.BAD_REQUEST, message);
    }

    public BizException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
