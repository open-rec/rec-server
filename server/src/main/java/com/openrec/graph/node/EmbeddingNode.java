package com.openrec.graph.node;

import static com.openrec.graph.RecParams.SCENE;

import java.util.ArrayList;
import java.util.List;

import org.assertj.core.util.Lists;

import com.openrec.graph.GraphContext;
import com.openrec.graph.config.EmbeddingConfig;
import com.openrec.graph.config.NodeConfig;
import com.openrec.graph.tools.anno.Export;
import com.openrec.graph.tools.anno.Import;
import com.openrec.proto.model.ScoreResult;
import com.openrec.service.recall.RecallStore;
import com.openrec.util.BeanUtil;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class EmbeddingNode extends RecallNode<EmbeddingConfig> {

    private RecallStore recallStore = BeanUtil.getBean(RecallStore.class);

    @Import("triggerItems")
    private List<ScoreResult> triggerItems;

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
        for (ScoreResult trigger : triggerItems) {
            triggers.add(trigger.getId());
        }
        String scene = context.getParams().getValueToString(SCENE);
        embeddingItems = recallStore.embedding(
            tableName(), scene, triggers, config.getContent().getSize(), config.getTimeout());
        exportChannel(context, embeddingItems);
        log.info("{} type:{} table:{} with embedding size:{}",
            getName(), recallType(), tableName(), embeddingItems.size());
    }
}
