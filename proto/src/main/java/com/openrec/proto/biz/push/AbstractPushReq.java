package com.openrec.proto.biz.push;

import java.util.List;

import lombok.Data;

@Data
public abstract class AbstractPushReq<T> {

    protected PushCmd cmd = PushCmd.INSERT;
    protected List<T> data;
}
