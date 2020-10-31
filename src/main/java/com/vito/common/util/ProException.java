package com.vito.common.util;

import lombok.Getter;

public class ProException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    @Getter
    private Integer code;
    @Getter
    private String msg;

    public ProException(Integer code) {
        this.code = code;
        this.msg = "Error";
    }

    public ProException(String format, Object... args) {
        this.code = -1;
        this.msg = String.format(format, args);
    }

    public ProException(Integer code, String format, Object... args) {
        this.code = code;
        this.msg = String.format(format, args);
    }

    public ProException(Throwable err, Integer code, String format, Object... args) {
        super(err);
        this.code = code;
        this.msg = String.format(format, args);
    }

    @Override
    public String getLocalizedMessage() {
        return "ERROR[" + code + "]: " + msg;
    }

}
