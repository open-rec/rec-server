package com.openrec.graph.config;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

public class NodeConfigType implements ParameterizedType {

    private final Type rawType;
    private final Type[] typeArguments;

    public NodeConfigType(Class config) {
        this.rawType = NodeConfig.class;
        this.typeArguments = new Type[1];
        this.typeArguments[0] = config;
    }

    @Override
    public Type[] getActualTypeArguments() {
        return typeArguments;
    }

    @Override
    public Type getRawType() {
        return rawType;
    }

    @Override
    public Type getOwnerType() {
        return null;
    }
}
