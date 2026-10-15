package com.openrec.graph.node.item;

import com.openrec.graph.node.*;

import static com.openrec.graph.RecParams.USER_ID;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.LinkedHashMap;
import java.util.HashMap;

import org.apache.commons.lang3.exception.ExceptionUtils;
import com.google.common.collect.Lists;

import com.openrec.graph.GraphContext;
import com.openrec.graph.config.NodeConfig;
import com.openrec.graph.config.RankConfig;
import com.openrec.graph.tools.anno.Export;
import com.openrec.graph.tools.anno.Import;
import com.openrec.proto.model.ScoreResult;
import com.openrec.service.rank.RankService;
import org.springframework.beans.factory.annotation.Autowired;

import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;

@Slf4j
public class RankNode extends AbstractSyncNode<RankConfig> {

    @Autowired
    private RankService rankService;

    private String bizType = "rank";

    @Import("combineItems")
    private List<ScoreResult> combineItems;

    @Import("userFeatureMap")
    private Map<String, String> userFeatureMap;

    @Export("rankItems")
    private List<ScoreResult> rankItems;

    public RankNode(NodeConfig nodeConfig) {
        super(nodeConfig);
        RankScoreFusion.validate(config.getContent().getScoreStrategy());
        this.rankItems = Lists.newArrayList();
    }

    @Override
    public void run(GraphContext context) {
        int size = config.getContent().getSize();
        int timeout = config.getTimeout();
        boolean open = config.isOpen();

        if (!open || !rankService.isOpen()) {
            rankItems = combineItems;
            log.info("{} or rank service not open, just return", getName());
            return;
        }

        rankItems = combineItems.subList(0, Math.min(size, combineItems.size()));
        if (CollectionUtils.isEmpty(rankItems)) {
            return;
        }

        // Snapshot the recall score before ranking overwrites it, so the two stages stay
        // distinguishable even when the rank engine is unreachable.
        for (ScoreResult itemScore : rankItems) {
            itemScore.setRecallScore(itemScore.getScore());
        }

        String userId = userFeatureMap.get(USER_ID);
        List<String> itemIds = rankItems.stream().map(ScoreResult::getId).collect(Collectors.toList());
        try {
            Map<String, Object> requestContext = new HashMap<>();
            requestContext.put("scene", context.getParams().getValueToString("scene"));
            requestContext.put("device_id", context.getParams().getValueToString("deviceId"));
            requestContext.put("request_time", System.currentTimeMillis() / 1000L);
            Map<String, Map<String, Object>> candidateContexts = new LinkedHashMap<>();
            for (int position = 0; position < rankItems.size(); position++) {
                ScoreResult candidate = rankItems.get(position);
                Map<String, Object> values = new HashMap<>();
                values.put("candidate_position", position);
                values.put("candidate_count", rankItems.size());
                values.put("recall_from", candidate.getRecallFrom());
                values.put("recall_score", candidate.getRecallScore());
                values.put("recall_scores", candidate.getRecallScores());
                candidateContexts.put(candidate.getId(), values);
            }
            Map<String, Double> rankResult = rankService.score(userId, itemIds, "item",
                context.getParams().getValueToString("sessionId"), requestContext, candidateContexts);
            for (ScoreResult itemScore : rankItems) {
                double rankScore = rankResult.getOrDefault(itemScore.getId(), 0d);
                itemScore.setRankScore(rankScore);
                itemScore
                    .setScore(RankScoreFusion.calculate(itemScore, rankScore, config.getContent().getScoreStrategy()));
            }
        } catch (Exception e) {
            // rankScore stays null on purpose: ranking did not happen, which differs from scoring 0
            log.warn("rank score failed with exception: {}", ExceptionUtils.getStackTrace(e));
        }
        log.info("{} with result size:{}", getName(), rankItems.size());
    }
}
