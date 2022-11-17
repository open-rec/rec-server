package com.openrec.proto;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

public class JsonResType implements ParameterizedType {

    private final Type rawType;
    private final Type[] typeArguments;

    public JsonResType(Class config) {
        this.rawType = JsonRes.class;
        this.typeArguments = new Type[] {config};
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
