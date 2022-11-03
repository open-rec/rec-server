package com.openrec.graph.node;

import com.openrec.graph.GraphContext;
import com.openrec.graph.config.RankConfig;
import com.openrec.graph.tools.anno.Export;
import com.openrec.graph.tools.anno.Import;
import com.openrec.proto.model.ScoreResult;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;

@Slf4j
public class RankNode extends SyncNode<RankConfig>{

    @Import("combineItems")
    private List<ScoreResult> combineItems;

    @Import("userFeatureMap")
    private Map<String,String> userFeatureMap;

    @Export("rankItems")
    private List<ScoreResult> rankItems;

    @Override
    public void run(GraphContext context) {
        int size = config.getContent().getSize();
        String rankServer = config.getContent().getEndpoint();
        rankItems = combineItems.subList(0, Math.min(size, combineItems.size()));
        // TODO: 2022/11/2
        log.info("{} with result size:{}", rankItems);
    }
}
