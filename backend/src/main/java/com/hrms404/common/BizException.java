package com.hrms404.common;

import lombok.Getter;

/**
 * 业务异常：携带业务错误码，由全局异常处理器统一转为 Result 返回
 */
@Getter
public class BizException extends RuntimeException {

    private final int code;

    public BizException(String message) {
        this(500, message);
    }

    public BizException(int code, String message) {
        super(message);
        this.code = code;
    }

    public static BizException badRequest(String msg) {
        return new BizException(400, msg);
    }

    public static BizException notFound(String msg) {
        return new BizException(404, msg);
    }

    public static BizException conflict(String msg) {
        return new BizException(409, msg);
    }

    public static BizException forbidden(String msg) {
        return new BizException(403, msg);
    }
}
