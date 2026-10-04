package com.openrec.graph.config;

import java.lang.reflect.Type;

import com.google.gson.reflect.TypeToken;

public class NodeConfigTool {

    public static Type getNodeConfigType(Class contentConfig) {
        return TypeToken.get(new NodeConfigType(contentConfig)).getType();
    }
}
