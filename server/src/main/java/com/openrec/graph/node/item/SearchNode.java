package com.openrec.graph.node.item;

import static com.openrec.graph.RecParams.SCENE;

import java.util.Collections;
import java.util.List;

import com.google.common.collect.Lists;
import org.springframework.beans.factory.annotation.Autowired;

import com.openrec.graph.GraphContext;
import com.openrec.graph.RequestTrigger;
import com.openrec.graph.config.NodeConfig;
import com.openrec.graph.config.SearchConfig;
import com.openrec.graph.node.AbstractRecallNode;
import com.openrec.graph.tools.anno.Export;
import com.openrec.proto.model.ScoreResult;
import com.openrec.service.recall.RecallStore;

import lombok.extern.slf4j.Slf4j;

/** BM25 recall driven by request text such as a search query or conversation turn. */
@Slf4j
public class SearchNode extends AbstractRecallNode<SearchConfig> {

    @Autowired
    private RecallStore recallStore;

    @Export("searchItems")
    private List<ScoreResult> searchItems;

    public SearchNode(NodeConfig nodeConfig) {
        super(nodeConfig);
        searchItems = Lists.newArrayList();
    }

    protected String entityField() {
        return "item";
    }

    @Override
    public void run(GraphContext context) {
        if (!config.isOpen()) {
            return;
        }
        String query =
            "query".equals(config.getContent().getQueryParam()) ? RequestTrigger.resolve(context).triggerQuery()
                : context.getParams().getValueToString(config.getContent().getQueryParam());
        if (query == null || query.trim().isEmpty()) {
            searchItems = Collections.emptyList();
        } else {
            searchItems = recallStore.textSearch(tableName(), context.getParams().getValueToString(SCENE), query,
                config.getContent().getTextField(), config.getContent().getMinimumShouldMatch(), entityField(),
                config.getContent().getSize(), config.getTimeout());
        }
        exportChannel(context, searchItems);
        log.info("{} type:{} table:{} text search recall size:{}", getName(), recallType(), tableName(),
            searchItems.size());
    }
}
