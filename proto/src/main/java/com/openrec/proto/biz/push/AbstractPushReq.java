package com.openrec.proto.biz.push;

import lombok.Data;

import java.util.List;

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

@Data
public abstract class AbstractPushReq <T> {
    protected PushCmd cmd = PushCmd.INSERT;
    protected PushType type;
    protected List<T> data;
}
