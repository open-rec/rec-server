package com.openrec.graph.node.item;

import static com.openrec.graph.RecParams.QUERY_EMBEDDING;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.google.common.collect.Lists;
import org.springframework.beans.factory.annotation.Autowired;

import com.openrec.graph.GraphContext;
import com.openrec.graph.RequestTrigger;
import com.openrec.graph.config.EmbeddingConfig;
import com.openrec.graph.config.NodeConfig;
import com.openrec.graph.node.AbstractRecallNode;
import com.openrec.graph.tools.anno.Export;
import com.openrec.proto.model.ScoreResult;
import com.openrec.service.recall.RecallStore;
import com.openrec.service.embedding.EmbeddingService;

import lombok.extern.slf4j.Slf4j;

/** ANN recall using a supplied vector or an optional online text embedding service. */
@Slf4j
public class QueryEmbeddingNode extends AbstractRecallNode<EmbeddingConfig> {

    @Autowired
    private RecallStore recallStore;

    @Autowired
    private EmbeddingService embeddingService;

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
        long started = System.nanoTime();
        List<Double> vector = vector(context.getParams().getValueToList(QUERY_EMBEDDING));
        if (vector.isEmpty() && embeddingService != null) {
            vector = embeddingService.embedQuery(RequestTrigger.resolve(context).triggerQuery(),
                Math.max(0, config.getTimeout() - 50));
        }
        long remaining = config.getTimeout() - (System.nanoTime() - started) / 1_000_000;
        if (vector.isEmpty() || remaining <= 0) {
            queryEmbeddingItems = Collections.emptyList();
            exportChannel(context, queryEmbeddingItems);
            return;
        }
        queryEmbeddingItems = recallStore.queryEmbedding(tableName(), vector, config.getContent().getSize(), remaining);
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
