package com.openrec.graph.node;

import java.lang.reflect.Constructor;

import com.openrec.graph.config.NodeConfig;

/** Compatibility factory for legacy graphs that still contain a Java class name. */
public final class ReflectiveNodeFactory implements NodeFactory {

    public static final String TYPE = "legacy-reflective";

    @Override
    public String type() {
        return TYPE;
    }

    @Override
    public void validate(NodeConfig config) {
        if (config.getClazz() == null || config.getClazz().trim().isEmpty())
            throw new IllegalArgumentException("legacy node requires class: " + config.getName());
        nodeClass(config);
    }

    @Override
    public Node create(NodeConfig config) {
        try {
            Constructor<? extends Node> constructor = nodeClass(config).getDeclaredConstructor(NodeConfig.class);
            constructor.setAccessible(true);
            Node node = constructor.newInstance(config);
            node.setConfig(config);
            return node;
        } catch (Exception error) {
            throw new IllegalArgumentException("cannot create node " + config.getName(), error);
        }
    }

    @SuppressWarnings("unchecked")
    private static Class<? extends Node> nodeClass(NodeConfig config) {
        try {
            Class<?> value = Class.forName(config.getClazz());
            if (!Node.class.isAssignableFrom(value))
                throw new IllegalArgumentException("node class does not implement Node: " + config.getClazz());
            return (Class<? extends Node>)value;
        } catch (ClassNotFoundException error) {
            throw new IllegalArgumentException("unknown node class: " + config.getClazz(), error);
        }
    }
}
