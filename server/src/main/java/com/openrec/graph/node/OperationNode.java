package com.openrec.graph.node;

import java.util.List;

import org.assertj.core.util.Lists;

import com.openrec.contrib.operation.OperationRule;
import com.openrec.graph.GraphContext;
import com.openrec.graph.config.NodeConfig;
import com.openrec.graph.config.OperationConfig;
import com.openrec.graph.tools.anno.Export;
import com.openrec.graph.tools.anno.Import;
import com.openrec.plugin.OperationRuleManager;
import com.openrec.proto.model.ScoreResult;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class OperationNode extends SyncNode<OperationConfig> {

    @Import("rankItems")
    private List<ScoreResult> rankItems;

    @Export("operationItems")
    private List<ScoreResult> operationItems;

    private OperationRule operationRule;

    public OperationNode(NodeConfig nodeConfig) {
        super(nodeConfig);
        this.operationItems = Lists.newArrayList();
        this.operationRule = OperationRuleManager.getOperationRuleByName(config.getContent().getOperationName());
    }

    @Override
    public void run(GraphContext context) {
        if (operationRule != null) {
            operationItems = operationRule.handle(context, rankItems);
            log.info("{}:{} exec finished", getName(), config.getContent().getOperationName());
        } else {
            operationItems = rankItems;
            log.warn("{} load {} failed, please check again", getName(), config.getContent().getOperationName());
        }
        log.info("{} with result size:{}", getName(), operationItems.size());
    }
}
