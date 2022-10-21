package com.openrec.graph.config;


import lombok.Data;

@Data
public abstract class NodeConfig<T> {

    protected boolean open;

    protected T content;

    @Override
    public String toString() {
        return super.toString();
    }
}
