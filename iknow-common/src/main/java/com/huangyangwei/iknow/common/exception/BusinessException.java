package com.huangyangwei.iknow.common.exception;

import com.huangyangwei.iknow.common.api.ResultCode;

/**
 * 业务异常：携带错误码，由全局异常处理器转为统一响应体。
 */
public class BusinessException extends RuntimeException {

    private final int code;

    public BusinessException(ResultCode resultCode) {
        super(resultCode.getMessage());
        this.code = resultCode.getCode();
    }

    public BusinessException(ResultCode resultCode, String message) {
        super(message);
        this.code = resultCode.getCode();
    }

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
