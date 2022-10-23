package com.openrec.proto;


import lombok.Data;

@Data
public class JsonRes<T> {
    protected int code;
    protected boolean status;
    protected String msg;
    protected T data;

    public JsonRes() {
        this(null);
    }

    public JsonRes(T data) {
        this("", data);
    }

    public JsonRes(String msg, T data) {
        this(true, msg, data);
    }

    public JsonRes(boolean status, String msg, T data) {
        this(ProtoCode.SUCCESS, status, msg, data);
    }

    public JsonRes(int code, boolean status, String msg, T data) {
        this.code = code;
        this.status = status;
        this.msg = msg;
        this.data = data;
    }
}
