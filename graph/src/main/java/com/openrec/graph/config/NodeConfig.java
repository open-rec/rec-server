package com.openrec.graph.config;

import lombok.Data;
import com.openrec.graph.node.FailurePolicy;

@Data
public class NodeConfig<T> {

    protected String name;
    protected boolean open;
    protected int timeout;
    protected String clazz;
    protected String configClazz;
    protected FailurePolicy failurePolicy = FailurePolicy.CONTINUE;

    protected T content;

    @Override
    public String toString() {
        return super.toString();
    }
}
