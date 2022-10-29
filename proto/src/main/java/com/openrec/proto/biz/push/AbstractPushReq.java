package com.openrec.proto.biz.push;

import lombok.Data;

import java.util.List;

@Data
public abstract class AbstractPushReq <T> {

    protected PushCmd cmd = PushCmd.INSERT;
    protected List<T> data;
}
