package com.openrec.graph.node.item;

import com.openrec.graph.node.*;

import static com.openrec.graph.RecParams.SCENE;

import java.util.ArrayList;
import java.util.List;

import com.google.common.collect.Lists;

import com.openrec.graph.GraphContext;
import com.openrec.graph.RequestTrigger;
import com.openrec.graph.config.EmbeddingConfig;
import com.openrec.graph.config.NodeConfig;
import com.openrec.graph.tools.anno.Export;
import com.openrec.graph.tools.anno.Import;
import com.openrec.proto.model.ScoreResult;
import com.openrec.service.recall.RecallStore;
import org.springframework.beans.factory.annotation.Autowired;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class EmbeddingNode extends AbstractRecallNode<EmbeddingConfig> {

    @Autowired
    private RecallStore recallStore;

    @Import(RequestTrigger.KEY)
    private RequestTrigger requestTrigger;

    @Export("embeddingItems")
    private List<ScoreResult> embeddingItems;

    public EmbeddingNode(NodeConfig nodeConfig) {
        super(nodeConfig);
        this.embeddingItems = Lists.newArrayList();
    }

    @Override
    public void run(GraphContext context) {
        if (!config.isOpen()) {
            log.info("{} not open, just return", getName());
            return;
        }
        List<String> triggers = new ArrayList<>();
        for (RequestTrigger.TriggerItem trigger : requestTrigger.triggerItems()) {
            triggers.add(trigger.id());
        }
        String scene = context.getParams().getValueToString(SCENE);
        embeddingItems =
            recallStore.embedding(tableName(), scene, triggers, config.getContent().getSize(), config.getTimeout());
        exportChannel(context, embeddingItems);
        log.info("{} type:{} table:{} with embedding size:{}", getName(), recallType(), tableName(),
            embeddingItems.size());
    }
}
