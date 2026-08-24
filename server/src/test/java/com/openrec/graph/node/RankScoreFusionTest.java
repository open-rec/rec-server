package com.openrec.graph.node;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;

import org.junit.Test;

import com.openrec.graph.config.RankChannelWeightConfig;
import com.openrec.graph.config.RankScoreStrategyConfig;
import com.openrec.proto.model.ScoreResult;

public class RankScoreFusionTest {

    @Test
    public void firstUsesConfiguredChannelOrderAndWeights() {
        ScoreResult item = itemWithAllRecallScores();
        RankScoreStrategyConfig strategy = strategy("first", 0.2, 0.9,
            channel("hot", 2), channel("i2i", 10));

        assertEquals(1.7, RankScoreFusion.calculate(item, 1, strategy), 0.000001);
        assertEquals(Double.valueOf(4), item.getRecallFusionScore());
    }

    @Test
    public void maxUsesLargestWeightedChannelValue() {
        ScoreResult item = itemWithAllRecallScores();
        RankScoreStrategyConfig strategy = strategy("max", 0.2, 0.9,
            channel("i2i", 2), channel("embedding", 1), channel("hot", 2));

        assertEquals(1.7, RankScoreFusion.calculate(item, 1, strategy), 0.000001);
        assertEquals(Double.valueOf(4), item.getRecallFusionScore());
    }

    @Test
    public void sumAddsEveryMatchedWeightedChannel() {
        ScoreResult item = itemWithAllRecallScores();
        RankScoreStrategyConfig strategy = strategy("sum", 0.4, 0.6,
            channel("i2i", 1), channel("embedding", 1), channel("hot", 1), channel("new", 1));

        assertEquals(4.6, RankScoreFusion.calculate(item, 1, strategy), 0.000001);
        assertEquals(Double.valueOf(10), item.getRecallFusionScore());
    }

    @Test
    public void absentStrategyPreservesLegacyPrimaryRecallScore() {
        ScoreResult item = itemWithAllRecallScores();

        assertEquals(6, RankScoreFusion.calculate(item, 5, null), 0.000001);
    }

    @Test
    public void configuredStrategyDefaultsToCurrentChannelOrderAndUnitWeights() {
        ScoreResult item = itemWithAllRecallScores();
        RankScoreStrategyConfig strategy = new RankScoreStrategyConfig();

        assertEquals(6, RankScoreFusion.calculate(item, 5, strategy), 0.000001);
        assertEquals(Double.valueOf(1), item.getRecallFusionScore());
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsUnsupportedAggregation() {
        RankScoreStrategyConfig strategy = strategy("average", 1, 1, channel("i2i", 1));
        RankScoreFusion.validate(strategy);
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsDuplicateChannels() {
        RankScoreStrategyConfig strategy = strategy("sum", 1, 1,
            channel("i2i", 1), channel("i2i", 2));
        RankScoreFusion.validate(strategy);
    }

    private static ScoreResult itemWithAllRecallScores() {
        ScoreResult item = new ScoreResult("item", 1);
        item.setRecallScore(1d);
        item.addRecallScore("i2i", 1);
        item.addRecallScore("embedding", 2);
        item.addRecallScore("hot", 2);
        item.addRecallScore("new", 5);
        return item;
    }

    private static RankScoreStrategyConfig strategy(String aggregation, double recallWeight,
        double rankWeight, RankChannelWeightConfig... channels) {
        RankScoreStrategyConfig strategy = new RankScoreStrategyConfig();
        strategy.setRecallAggregation(aggregation);
        strategy.setRecallWeight(recallWeight);
        strategy.setRankWeight(rankWeight);
        strategy.setChannels(Arrays.asList(channels));
        return strategy;
    }

    private static RankChannelWeightConfig channel(String name, double weight) {
        return new RankChannelWeightConfig(name, weight);
    }
}
