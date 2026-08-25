package com.openrec.contrib.operation.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.junit.Test;

import com.openrec.graph.GraphContext;
import com.openrec.graph.config.NodeConfig;
import com.openrec.graph.config.OperationConfig;
import com.openrec.proto.model.ScoreResult;

public class ChannelOperationRuleTest {

    @Test
    public void weightedRuleEnforcesRatioAndKeepsHighestScores() {
        GraphContext context = context(10,
            ratios("item_cf_i2i", 0.3, "item_seq_emb", 0.3, "hot", 0.2, "new", 0.2), null);
        List<ScoreResult> input = new ArrayList<>();
        add(input, "item_cf_i2i", 5, 0.50);
        add(input, "item_seq_emb", 5, 0.60);
        add(input, "hot", 5, 0.90);
        add(input, "new", 5, 1.00);

        List<ScoreResult> result = new WeightedChannelOperationRule().handle(context, input);

        assertEquals(10, result.size());
        assertEquals(3, count(result, "item_cf_i2i"));
        assertEquals(3, count(result, "item_seq_emb"));
        assertEquals(2, count(result, "hot"));
        assertEquals(2, count(result, "new"));
        for (int i = 1; i < result.size(); i++) {
            assertTrue(result.get(i - 1).getScore() >= result.get(i).getScore());
        }
        assertEquals("new-0", result.get(0).getId());
    }

    @Test
    public void weightedRuleFillsQuotaShortageWithHighestRemainingCandidates() {
        GraphContext context = context(4, ratios("item_cf_i2i", 0.5, "new", 0.5), null);
        List<ScoreResult> input = new ArrayList<>();
        add(input, "item_cf_i2i", 4, 0.80);
        add(input, "new", 1, 1.00);

        List<ScoreResult> result = new WeightedChannelOperationRule().handle(context, input);

        assertEquals(4, result.size());
        assertEquals(1, count(result, "new"));
        assertEquals(3, count(result, "item_cf_i2i"));
    }

    @Test
    public void weightedRuleRoundsStandaloneSizeToExpectedQuotas() {
        Map<String, Integer> quotas = ChannelMixSupport.proportionalQuotas(
            ratios("item_cf_i2i", 0.3, "item_seq_emb", 0.3, "hot", 0.2, "new", 0.2), 12);

        assertEquals(Integer.valueOf(4), quotas.get("item_cf_i2i"));
        assertEquals(Integer.valueOf(4), quotas.get("item_seq_emb"));
        assertEquals(Integer.valueOf(2), quotas.get("hot"));
        assertEquals(Integer.valueOf(2), quotas.get("new"));
    }

    @Test
    public void randomRuleGuaranteesConfiguredCandidatesAtRandomPositions() {
        GraphContext context = context(10, null, ratios("hot", 0.1, "new", 0.1));
        List<ScoreResult> input = new ArrayList<>();
        add(input, "item_cf_i2i", 8, 0.80);
        add(input, "hot", 3, 0.95);
        add(input, "new", 3, 1.00);

        List<ScoreResult> result =
            new RandomInsertOperationRule(new Random(7)).handle(context, input);

        assertEquals(10, result.size());
        assertEquals(1, count(result, "hot"));
        assertEquals(1, count(result, "new"));
        assertTrue(result.indexOf(find(result, "hot")) != 8
            || result.indexOf(find(result, "new")) != 9);
    }

    private static GraphContext context(int size, Map<String, Double> ratios,
        Map<String, Double> randomRatios) {
        OperationConfig operation = new OperationConfig();
        operation.setChannelRatios(ratios);
        operation.setRandomInsertRatios(randomRatios);
        NodeConfig<OperationConfig> node = new NodeConfig<>();
        node.setContent(operation);
        GraphContext context = new GraphContext();
        context.addParam("size", size);
        context.addConfig("operation", node);
        return context;
    }

    private static Map<String, Double> ratios(Object... entries) {
        Map<String, Double> result = new LinkedHashMap<>();
        for (int i = 0; i < entries.length; i += 2) {
            result.put((String)entries[i], (Double)entries[i + 1]);
        }
        return result;
    }

    private static void add(List<ScoreResult> target, String channel, int count, double topScore) {
        for (int i = 0; i < count; i++) {
            ScoreResult item = new ScoreResult(channel + "-" + i, topScore - i * 0.01);
            item.setRecallFrom(channel);
            target.add(item);
        }
    }

    private static long count(List<ScoreResult> items, String channel) {
        return items.stream().filter(item -> channel.equals(item.getRecallFrom())).count();
    }

    private static ScoreResult find(List<ScoreResult> items, String channel) {
        return items.stream().filter(item -> channel.equals(item.getRecallFrom())).findFirst().get();
    }
}
