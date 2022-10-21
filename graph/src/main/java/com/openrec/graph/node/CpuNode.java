package com.openrec.graph.node;

import com.openrec.graph.GraphContext;

public abstract class CpuNode extends AbstractNode {


    public abstract void run(GraphContext context);
}
