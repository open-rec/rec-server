package com.openrec.graph.node.item;

import static com.openrec.graph.RecParams.SCENE;

import java.util.Collections;
import java.util.List;

import org.assertj.core.util.Lists;
import org.springframework.beans.factory.annotation.Autowired;

import com.openrec.graph.GraphContext;
import com.openrec.graph.config.NodeConfig;
import com.openrec.graph.config.SparseConfig;
import com.openrec.graph.node.AbstractRecallNode;
import com.openrec.graph.tools.anno.Export;
import com.openrec.proto.model.ScoreResult;
import com.openrec.service.recall.RecallStore;

import lombok.extern.slf4j.Slf4j;

/** BM25 recall driven by request text such as a search query or conversation turn. */
@Slf4j
public class SparseNode extends AbstractRecallNode<SparseConfig> {

    @Autowired
    private RecallStore recallStore;

    @Export("sparseItems")
    private List<ScoreResult> sparseItems;

    public SparseNode(NodeConfig nodeConfig) {
        super(nodeConfig);
        sparseItems = Lists.newArrayList();
    }

    @Override
    public void run(GraphContext context) {
        if (!config.isOpen()) {
            return;
        }
        String query = context.getParams().getValueToString(config.getContent().getQueryParam());
        if (query == null || query.trim().isEmpty()) {
            sparseItems = Collections.emptyList();
        } else {
            sparseItems = recallStore.sparse(tableName(), context.getParams().getValueToString(SCENE), query,
                config.getContent().getTextField(), config.getContent().getMinimumShouldMatch(),
                config.getContent().getSize(), config.getTimeout());
        }
        exportChannel(context, sparseItems);
        log.info("{} type:{} table:{} sparse recall size:{}", getName(), recallType(), tableName(), sparseItems.size());
    }
}
