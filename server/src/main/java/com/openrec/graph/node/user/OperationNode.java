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
    @Import("rankUsers")
    private List<ScoreResult> rankUsers;
    @Export("operationUsers")
    private List<ScoreResult> operationUsers = new ArrayList<>();
    private final OperationRule operationRule;

    public OperationNode(NodeConfig nodeConfig) {
        super(nodeConfig);
        operationRule = OperationRuleManager.getOperationRuleByName(config.getContent().getOperationName());
    }

    @Override
    public void run(GraphContext context) {
        operationUsers = operationRule == null ? rankUsers : operationRule.handle(context, rankUsers);
    }
}
