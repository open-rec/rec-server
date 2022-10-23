package com.openrec.proto;


import lombok.Data;

@Data
public abstract class JsonRes<T> {
    protected int code;
    protected boolean status;
    protected String msg;
    protected T data;
}
