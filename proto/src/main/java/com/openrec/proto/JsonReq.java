package com.openrec.proto;

import java.util.UUID;

import lombok.Data;

@Data
public class JsonReq<T> {
    protected String requestId;
    protected T body;

    public JsonReq() {
        this(null);
    }

    public JsonReq(T body) {
        this.requestId = UUID.randomUUID().toString();
        this.body = body;
    }
}
