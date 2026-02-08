package com.tslnkk.skcapi.exception;

import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;
    private final String field;
    private final Object rejectedValue;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getDefaultMessage());
        this.errorCode = errorCode;
        this.field = null;
        this.rejectedValue = null;
    }

    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
        this.field = null;
        this.rejectedValue = null;
    }

    public BusinessException(ErrorCode errorCode, String field, Object rejectedValue) {
        super(errorCode.getDefaultMessage());
        this.errorCode = errorCode;
        this.field = field;
        this.rejectedValue = rejectedValue;
    }

    public BusinessException(ErrorCode errorCode, String message, String field, Object rejectedValue) {
        super(message);
        this.errorCode = errorCode;
        this.field = field;
        this.rejectedValue = rejectedValue;
    }
}
