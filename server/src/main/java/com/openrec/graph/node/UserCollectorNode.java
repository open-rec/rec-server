package com.openrec.graph.node;

import java.util.List;
import com.openrec.graph.GraphContext;
import com.openrec.graph.config.NodeConfig;
import com.openrec.graph.tools.anno.Import;
import com.openrec.proto.model.ScoreResult;

/** Terminates a user recommendation graph without writing item exposure events. */
public class UserCollectorNode extends AbstractCollectorNode {
    @Import("userCandidates") private List<ScoreResult> candidates;
    public UserCollectorNode(NodeConfig nodeConfig) { super(nodeConfig); }
    @Override protected List<ScoreResult> candidates() { return candidates; }
}
