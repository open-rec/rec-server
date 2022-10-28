package com.openrec.proto;


import lombok.Data;

import java.util.UUID;

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
