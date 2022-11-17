package com.openrec.graph.node;

import java.util.Map;

import com.openrec.graph.GraphContext;

/**
 * for NIO
 *
 * @param <T>
 */
public abstract class AsyncNode<T> extends AbstractNode {

    public abstract Map<String, String> buildQuery(GraphContext context);

    public abstract void handleResult(GraphContext context, T result);
}
