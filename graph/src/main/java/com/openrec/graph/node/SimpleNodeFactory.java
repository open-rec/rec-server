package com.openrec.graph.node;

import java.util.function.Function;

import com.openrec.graph.config.NodeConfig;

/** Small factory implementation for explicit constructor registrations. */
public final class SimpleNodeFactory implements NodeFactory {
    private final String type;
    private final Function<NodeConfig, Node> creator;

    public SimpleNodeFactory(String type, Function<NodeConfig, Node> creator) {
        this.type = type;
        this.creator = creator;
    }

    @Override
    public String type() {
        return type;
    }

    @Override
    public Node create(NodeConfig config) {
        return creator.apply(config);
    }
}
