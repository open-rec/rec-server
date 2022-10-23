package com.openrec.proto.biz.push;

enum PushCmd {
    INSERT,
    UPDATE,
    DELETE;
}

enum PushType {
    ITEM,
    USER,
    EVENT
}

public abstract class AbstractPushReq <T> {
    protected PushCmd cmd = PushCmd.INSERT;
    protected PushType type;
    protected T data;
}
