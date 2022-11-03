package com.openrec.graph.node;

import com.openrec.graph.GraphContext;
import com.openrec.graph.config.NodeConfig;
import com.openrec.graph.tools.anno.Export;
import com.openrec.graph.tools.anno.Import;
import com.openrec.proto.model.ScoreResult;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public class FakeExposureNode extends SyncNode<Void> {

    @Import("operationItems")
    @Export("operationItems")
    private List<ScoreResult> operationItems;

    public FakeExposureNode(NodeConfig nodeConfig) {
        super(nodeConfig);
    }


    @Override
    public void run(GraphContext context) {
        // TODO: 2022/11/3 write fake exposure
        log.info("{} write fake exposure finished.", getName());
    }
}
