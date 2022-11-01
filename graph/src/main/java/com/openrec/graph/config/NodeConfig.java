package com.openrec.graph.config;


import lombok.Data;

@Data
public class NodeConfig<T> {

    protected String name;
    protected boolean open;
    protected int timeout;
    protected String clazz;

    protected T content;

    @Override
    public String toString() {
        return super.toString();
    }
}
