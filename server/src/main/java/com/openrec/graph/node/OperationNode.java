package com.openrec.graph.node;

import com.openrec.graph.GraphContext;
import com.openrec.graph.config.NodeConfig;
import com.openrec.graph.tools.anno.Export;
import com.openrec.graph.tools.anno.Import;
import com.openrec.proto.model.ScoreResult;
import lombok.extern.slf4j.Slf4j;

import java.util.List;


@Slf4j
public class OperationNode extends SyncNode<Void> {

    @Import("rankItems")
    private List<ScoreResult> rankItems;

    @Export("operationItems")
    private List<ScoreResult> operationItems;

    public OperationNode(NodeConfig nodeConfig) {
        super(nodeConfig);
    }


    @Override
    public void run(GraphContext context) {
        // TODO: 2022/11/2 for custom operation.
        operationItems = rankItems;
        log.info("{} with result size:{}", getName(), operationItems.size());
    }
}
