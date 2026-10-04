package com.openrec.graph.node.item;

import static com.openrec.graph.RecParams.QUERY_EMBEDDING;
import static com.openrec.graph.RecParams.SCENE;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.google.common.collect.Lists;
import org.springframework.beans.factory.annotation.Autowired;

import com.openrec.graph.GraphContext;
import com.openrec.graph.config.EmbeddingConfig;
import com.openrec.graph.config.NodeConfig;
import com.openrec.graph.node.AbstractRecallNode;
import com.openrec.graph.tools.anno.Export;
import com.openrec.proto.model.ScoreResult;
import com.openrec.service.recall.RecallStore;

import lombok.extern.slf4j.Slf4j;

/** ANN recall driven by a dense query vector supplied with the recommendation request. */
@Slf4j
public class QueryEmbeddingNode extends AbstractRecallNode<EmbeddingConfig> {

    @Autowired
    private RecallStore recallStore;

    @Export("queryEmbeddingItems")
    private List<ScoreResult> queryEmbeddingItems;

    public QueryEmbeddingNode(NodeConfig nodeConfig) {
        super(nodeConfig);
        queryEmbeddingItems = Lists.newArrayList();
    }

    @Override
    public void run(GraphContext context) {
        if (!config.isOpen()) {
            return;
        }
        List<Double> vector = vector(context.getParams().getValueToList(QUERY_EMBEDDING));
        if (vector.isEmpty()) {
            queryEmbeddingItems = Collections.emptyList();
            exportChannel(context, queryEmbeddingItems);
            return;
        }
        queryEmbeddingItems = recallStore.queryEmbedding(tableName(), context.getParams().getValueToString(SCENE),
            vector, config.getContent().getSize(), config.getTimeout());
        exportChannel(context, queryEmbeddingItems);
        log.info("{} type:{} table:{} with query embedding size:{}", getName(), recallType(), tableName(),
            queryEmbeddingItems.size());
    }

    static List<Double> vector(List<?> values) {
        if (values == null || values.isEmpty()) {
            return Collections.emptyList();
        }
        List<Double> result = new ArrayList<>(values.size());
        for (Object value : values) {
            if (!(value instanceof Number) || !Double.isFinite(((Number)value).doubleValue())) {
                return Collections.emptyList();
            }
            result.add(((Number)value).doubleValue());
        }
        return result;
    }
}
