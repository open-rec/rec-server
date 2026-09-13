package com.openrec.graph.node.user;

import static com.openrec.graph.RecParams.USER_ID;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.assertj.core.util.Lists;
import org.springframework.util.CollectionUtils;

import com.openrec.graph.GraphContext;
import com.openrec.graph.config.NodeConfig;
import com.openrec.graph.config.RankConfig;
import com.openrec.graph.tools.anno.Export;
import com.openrec.graph.tools.anno.Import;
import com.openrec.graph.node.RankScoreFusion;
import com.openrec.graph.node.AbstractSyncNode;
import com.openrec.proto.model.ScoreResult;
import com.openrec.service.rank.RankService;
import org.springframework.beans.factory.annotation.Autowired;

import lombok.extern.slf4j.Slf4j;

/** Scores candidate users with the user-target rank model. */
@Slf4j
public class RankNode extends AbstractSyncNode<RankConfig> {
    @Autowired
    private RankService rankService;

    @Import("userCandidates")
    private List<ScoreResult> userCandidates;

    @Export("rankUsers")
    private List<ScoreResult> rankUsers;

    public RankNode(NodeConfig nodeConfig) {
        super(nodeConfig);
        RankScoreFusion.validate(config.getContent().getScoreStrategy());
        rankUsers = Lists.newArrayList();
    }

    @Override
    public void run(GraphContext context) {
        if (!config.isOpen() || !rankService.isOpen()) {
            rankUsers = userCandidates;
            return;
        }
        rankUsers = userCandidates.subList(0, Math.min(config.getContent().getSize(), userCandidates.size()));
        if (CollectionUtils.isEmpty(rankUsers))
            return;
        for (ScoreResult candidate : rankUsers)
            candidate.setRecallScore(candidate.getScore());
        String userId = context.getParams().getValueToString(USER_ID);
        List<String> ids = rankUsers.stream().map(ScoreResult::getId).collect(Collectors.toList());
        try {
            Map<String, Double> scores = rankService.score(userId, ids, "user");
            for (ScoreResult candidate : rankUsers) {
                double rankScore = scores.getOrDefault(candidate.getId(), 0d);
                candidate.setRankScore(rankScore);
                candidate
                    .setScore(RankScoreFusion.calculate(candidate, rankScore, config.getContent().getScoreStrategy()));
            }
        } catch (Exception error) {
            log.warn("user rank failed with exception: {}", ExceptionUtils.getStackTrace(error));
        }
    }
}
