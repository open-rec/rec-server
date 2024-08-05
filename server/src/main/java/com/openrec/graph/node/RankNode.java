package com.openrec.graph.node;

import static com.openrec.graph.RecParams.USER_ID;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.assertj.core.util.Lists;

import com.openrec.graph.GraphContext;
import com.openrec.graph.config.NodeConfig;
import com.openrec.graph.config.RankConfig;
import com.openrec.graph.tools.anno.Export;
import com.openrec.graph.tools.anno.Import;
import com.openrec.proto.model.ScoreResult;
import com.openrec.service.rank.RankService;
import com.openrec.util.BeanUtil;

import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;

@Slf4j
public class RankNode extends SyncNode<RankConfig> {

    private RankService rankService = BeanUtil.getBean(RankService.class);

    private String bizType = "rank";

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

        rankItems = combineItems.subList(0, Math.min(size, combineItems.size()));
        if(CollectionUtils.isEmpty(rankItems)) {
            return;
        }

        String userId = userFeatureMap.get(USER_ID);
        List<String> itemIds = rankItems.stream().map(ScoreResult::getId).collect(Collectors.toList());
        Map<String, Double> rankResult = rankService.score(userId, itemIds);
        for (ScoreResult itemScore : rankItems) {
            // TODO: 2024/8/5 provide a simple weight fusion function.
            itemScore.setScore(itemScore.getScore() + rankResult.getOrDefault(itemScore.getId(), 0d));
        }
        log.info("{} with result size:{}", getName(), rankItems.size());
    }
}
