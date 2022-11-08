package com.openrec.graph.node;

import com.openrec.graph.GraphContext;
import com.openrec.graph.config.NodeConfig;
import com.openrec.graph.config.RankConfig;
import com.openrec.graph.tools.anno.Export;
import com.openrec.graph.tools.anno.Import;
import com.openrec.proto.model.ScoreResult;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.util.Lists;

import java.util.List;
import java.util.Map;

@Slf4j
public class RankNode extends SyncNode<RankConfig> {

    @Import("combineItems")
    private List<ScoreResult> combineItems;

    @Import("userFeatureMap")
    private Map<String, String> userFeatureMap;

    @Export("rankItems")
    private List<ScoreResult> rankItems;

    public RankNode(NodeConfig nodeConfig) {
        super(nodeConfig);
        this.rankItems = Lists.newArrayList();
    }


    @Override
    public void run(GraphContext context) {
        int size = config.getContent().getSize();
        int timeout = config.getTimeout();
        boolean open = config.isOpen();

        if (!open) {
            rankItems = combineItems;
            log.info("{} not open, just return", getName());
            return;
        }

        String rankServer = config.getContent().getEndpoint();
        rankItems = combineItems.subList(0, Math.min(size, combineItems.size()));
        // TODO: 2022/11/2 add tf-serving later.
        rankItems.addAll(combineItems.subList(rankItems.size(), combineItems.size()));
        log.info("{} with result size:{}", getName(), rankItems);
    }
}
