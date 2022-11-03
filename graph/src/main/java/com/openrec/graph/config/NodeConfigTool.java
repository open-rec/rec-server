package com.openrec.graph.config;

import com.google.gson.internal.$Gson$Preconditions;
import com.google.gson.internal.$Gson$Types;

import java.lang.reflect.Type;

public class NodeConfigTool {

    public static Type getNodeConfigType(Class contentConfig) {
        return $Gson$Types.canonicalize($Gson$Preconditions.checkNotNull(new NodeConfigType(contentConfig)));
    }
}
