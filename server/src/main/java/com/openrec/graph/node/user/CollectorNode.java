package com.openrec.graph.node.user;

import java.util.List;
import com.openrec.graph.GraphContext;
import com.openrec.graph.config.NodeConfig;
import com.openrec.graph.tools.anno.Import;
import com.openrec.proto.model.ScoreResult;
import com.openrec.graph.node.AbstractCollectorNode;

/** Terminates a user recommendation graph without writing item exposure events. */
public class CollectorNode extends AbstractCollectorNode {
    @Import("operationUsers")
    private List<ScoreResult> candidates;

    public CollectorNode(NodeConfig nodeConfig) {
        super(nodeConfig);
    }

    @Override
    protected List<ScoreResult> candidates() {
        return candidates;
    }
}
