package com.openrec.graph.node;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.openrec.graph.config.RankChannelWeightConfig;
import com.openrec.graph.config.RankScoreStrategyConfig;
import com.openrec.proto.model.ScoreResult;

/** Calculates the final online score while keeping score policy out of {@link RankNode}. */
final class RankScoreFusion {

    private static final List<RankChannelWeightConfig> DEFAULT_CHANNELS = Arrays.asList(
        new RankChannelWeightConfig(CombineNode.CHANNEL_I2I, 1d),
        new RankChannelWeightConfig(CombineNode.CHANNEL_EMBEDDING, 1d),
        new RankChannelWeightConfig(CombineNode.CHANNEL_HOT, 1d),
        new RankChannelWeightConfig(CombineNode.CHANNEL_NEW, 1d));

    private RankScoreFusion() {}

    static double calculate(ScoreResult item, double rankScore, RankScoreStrategyConfig strategy) {
        if (strategy == null) {
            return item.getRecallScore() + rankScore;
        }
        double recallFusionScore = aggregate(item, strategy);
        item.setRecallFusionScore(recallFusionScore);
        return recallFusionScore * strategy.getRecallWeight() + rankScore * strategy.getRankWeight();
    }

    static void validate(RankScoreStrategyConfig strategy) {
        if (strategy == null) {
            return;
        }
        String aggregation = normalizedAggregation(strategy);
        if (!"first".equals(aggregation) && !"max".equals(aggregation) && !"sum".equals(aggregation)) {
            throw new IllegalArgumentException("unsupported recallAggregation: " + strategy.getRecallAggregation());
        }
        requireNonNegativeFinite("recallWeight", strategy.getRecallWeight());
        requireNonNegativeFinite("rankWeight", strategy.getRankWeight());
        if (strategy.getRecallWeight() == 0d && strategy.getRankWeight() == 0d) {
            throw new IllegalArgumentException("recallWeight and rankWeight cannot both be zero");
        }
        Set<String> names = new HashSet<>();
        for (RankChannelWeightConfig channel : channels(strategy)) {
            if (channel == null || channel.getName() == null || channel.getName().trim().isEmpty()) {
                throw new IllegalArgumentException("rank score channel name cannot be empty");
            }
            if (!names.add(channel.getName())) {
                throw new IllegalArgumentException("duplicate rank score channel: " + channel.getName());
            }
            requireNonNegativeFinite("channel weight for " + channel.getName(), channel.getWeight());
        }
    }

    private static double aggregate(ScoreResult item, RankScoreStrategyConfig strategy) {
        Map<String, Double> scores = item.getRecallScores();
        if (scores == null || scores.isEmpty()) {
            return item.getRecallScore();
        }
        String aggregation = normalizedAggregation(strategy);
        Double result = null;
        for (RankChannelWeightConfig channel : channels(strategy)) {
            if (!scores.containsKey(channel.getName())) {
                continue;
            }
            double channelValue = scores.get(channel.getName()) * channel.getWeight();
            if ("first".equals(aggregation)) {
                return channelValue;
            }
            if ("max".equals(aggregation)) {
                result = result == null ? channelValue : Math.max(result, channelValue);
            } else {
                result = result == null ? channelValue : result + channelValue;
            }
        }
        return result == null ? 0d : result;
    }

    private static List<RankChannelWeightConfig> channels(RankScoreStrategyConfig strategy) {
        return strategy.getChannels() == null || strategy.getChannels().isEmpty()
            ? DEFAULT_CHANNELS : strategy.getChannels();
    }

    private static String normalizedAggregation(RankScoreStrategyConfig strategy) {
        return strategy.getRecallAggregation() == null
            ? "first" : strategy.getRecallAggregation().trim().toLowerCase();
    }

    private static void requireNonNegativeFinite(String name, double value) {
        if (Double.isNaN(value) || Double.isInfinite(value) || value < 0d) {
            throw new IllegalArgumentException(name + " must be a finite non-negative number");
        }
    }
}
