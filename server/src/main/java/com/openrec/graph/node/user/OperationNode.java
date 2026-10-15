package com.openrec.graph.node.user;

import java.util.ArrayList;
import java.util.List;

import com.openrec.contrib.operation.OperationRule;
import com.openrec.graph.GraphContext;
import com.openrec.graph.config.NodeConfig;
import com.openrec.graph.config.OperationConfig;
import com.openrec.graph.node.AbstractSyncNode;
import com.openrec.graph.tools.anno.Export;
import com.openrec.graph.tools.anno.Import;
import com.openrec.plugin.OperationRuleManager;
import com.openrec.proto.model.ScoreResult;

/** Applies an optional generic operation rule to ranked users. */
public class OperationNode extends AbstractSyncNode<OperationConfig> {
    @Import(value = "rankUsers", required = false)
    private List<ScoreResult> rankUsers;
    @Import(value = "userCandidates", required = false)
    private List<ScoreResult> userCandidates;

    @Export("operationUsers")
    private List<ScoreResult> operationUsers = new ArrayList<>();
    private final OperationRule operationRule;

    public OperationNode(NodeConfig nodeConfig) {
        super(nodeConfig);
        operationRule = OperationRuleManager.getOperationRuleByName(config.getContent().getOperationName());
    }

    @Override
    public void run(GraphContext context) {
        // A failed or timed-out rank node publishes no output. Preserve recall results.
        if (rankUsers == null)
            rankUsers = userCandidates;
        if (rankUsers == null)
            throw new IllegalStateException("no ranked or recall candidates");
        operationUsers = operationRule == null ? rankUsers : operationRule.handle(context, rankUsers);
    }
}
