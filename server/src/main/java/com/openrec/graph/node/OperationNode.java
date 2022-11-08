package com.openrec.graph.node;

import com.openrec.graph.GraphContext;
import com.openrec.graph.config.NodeConfig;
import com.openrec.graph.contrib.operation.DefaultOperationRule;
import com.openrec.graph.contrib.operation.OperationRule;
import com.openrec.graph.tools.anno.Export;
import com.openrec.graph.tools.anno.Import;
import com.openrec.proto.model.ScoreResult;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.util.Lists;

import java.util.List;


@Slf4j
public class OperationNode extends SyncNode<Void> {

    @Import("rankItems")
    private List<ScoreResult> rankItems;

    @Export("operationItems")
    private List<ScoreResult> operationItems;

    private OperationRule operationRule;

    public OperationNode(NodeConfig nodeConfig) {
        super(nodeConfig);
        this.operationItems = Lists.newArrayList();
        this.operationRule = new DefaultOperationRule();
    }


    @Override
    public void run(GraphContext context) {
        // for custom operation, default pass.
        operationItems = operationRule.handle(context, rankItems);
        log.info("{} with result size:{}", getName(), operationItems.size());
    }
}
