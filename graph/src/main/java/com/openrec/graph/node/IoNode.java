package com.openrec.graph.node;

import com.openrec.graph.GraphContext;

import java.util.Map;

public abstract class IoNode<T> extends AbstractNode {


    public abstract Map<String, String> buildQuery(GraphContext context);

    public abstract void handleResult(GraphContext context, T result);
}
