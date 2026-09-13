package com.openrec.graph.node;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import com.openrec.graph.config.NodeConfig;

/** Immutable registry that decouples graph configuration from Java constructors. */
public final class NodeRegistry {

    private final Map<String, NodeFactory> factories;
    private final NodeFactory fallback;

    private NodeRegistry(Map<String, NodeFactory> factories, NodeFactory fallback) {
        this.factories = Collections.unmodifiableMap(new LinkedHashMap<>(factories));
        this.fallback = fallback;
    }

    public static Builder builder() {
        return new Builder();
    }

    public NodeFactory resolve(NodeConfig config) {
        String type = config.getType();
        NodeFactory factory = type == null ? null : factories.get(type);
        if (factory == null && config.getClazz() != null)
            factory = factories.get(config.getClazz());
        if (factory == null && fallback != null)
            factory = fallback;
        if (factory == null)
            throw new IllegalArgumentException("unknown node type: " + type);
        factory.validate(config);
        return factory;
    }

    public static final class Builder {
        private final Map<String, NodeFactory> factories = new LinkedHashMap<>();
        private NodeFactory fallback;

        public Builder register(NodeFactory factory) {
            if (factory == null || factory.type() == null || factory.type().trim().isEmpty())
                throw new IllegalArgumentException("node factory requires a type");
            if (factories.put(factory.type(), factory) != null)
                throw new IllegalArgumentException("duplicate node factory: " + factory.type());
            return this;
        }

        public Builder fallback(NodeFactory factory) {
            this.fallback = factory;
            return this;
        }

        public NodeRegistry build() {
            return new NodeRegistry(factories, fallback);
        }
    }
}
