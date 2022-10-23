package com.openrec.proto;


import lombok.Data;

@Data
public class JsonReq<T> {
    protected String requestId;
    protected T body;
}
