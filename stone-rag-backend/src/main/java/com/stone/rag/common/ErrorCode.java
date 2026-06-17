package com.stone.rag.common;

import lombok.Getter;

@Getter
public enum ErrorCode {
    SUCCESS(200, "success"),
    BAD_REQUEST(40000, "参数错误"),
    NOT_FOUND(40001, "资源不存在"),
    UNAUTHORIZED(40002, "无权限"),
    RATE_LIMIT(40003, "频率限制"),
    FILE_ERROR(40004, "文件过大/类型不支持"),
    SERVER_ERROR(50000, "服务器内部错误");

    private final Integer code;
    private final String message;

    ErrorCode(Integer code, String message) {
        this.code = code;
        this.message = message;
    }

}