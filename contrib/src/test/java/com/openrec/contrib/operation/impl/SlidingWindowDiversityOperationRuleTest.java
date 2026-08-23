package com.openrec.contrib.operation.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import com.openrec.graph.GraphContext;
import com.openrec.graph.config.NodeConfig;
import com.openrec.graph.config.OperationConfig;
import com.openrec.proto.model.Item;
import com.openrec.proto.model.ScoreResult;

public class SlidingWindowDiversityOperationRuleTest {
    @Test
    public void scattersCategoryWithinEveryWindow() {
        GraphContext context = context(5, 3, 1, "category");
        List<ScoreResult> input = scores("a1", "a2", "a3", "b1", "c1", "d1");
        context.addData("operationItemMap", items(
            item("a1", "a", "x"), item("a2", "a", "y"), item("a3", "a", "z"),
            item("b1", "b", "x"), item("c1", "c", "x"), item("d1", "d", "x")));

        List<ScoreResult> result = new SlidingWindowDiversityOperationRule().handle(context, input);

        assertEquals(Arrays.asList("a1", "b1", "c1", "a2", "d1"), ids(result));
        assertWindowLimit(result, (Map<String, Item>)context.getData("operationItemMap"), 3, 1,
            Collections.singletonList("category"));
    }

    @Test
    public void appliesEveryTagAndCompositeDimensions() {
        Item item = item("i", "sports", "nba,cba");
        assertEquals(2, SlidingWindowDiversityOperationRule.keys(item,
            Collections.singletonList("tags")).size());
        assertTrue(SlidingWindowDiversityOperationRule.keys(item,
            Collections.singletonList("category+tags")).contains("category+tags=sports+nba"));

        GraphContext context = context(3, 3, 1, "category+tags");
        List<ScoreResult> input = scores("a", "b", "c");
        context.addData("operationItemMap", items(item("a", "sports", "nba"),
            item("b", "sports", "cba"), item("c", "sports", "nba")));
        assertEquals(Arrays.asList("a", "b"), ids(
            new SlidingWindowDiversityOperationRule().handle(context, input)));
    }

    @Test
    public void returnsShorterResultWhenConstraintCannotBeSatisfied() {
        GraphContext context = context(3, 3, 1, "category");
        List<ScoreResult> input = scores("a", "b", "c");
        context.addData("operationItemMap", items(item("a", "same", "x"),
            item("b", "same", "y"), item("c", "same", "z")));
        assertEquals(1, new SlidingWindowDiversityOperationRule().handle(context, input).size());
    }

    @Test
    public void doesNotApplyChannelRatios() {
        GraphContext context = context(4, 4, 4, "category");
        OperationConfig operation = (OperationConfig)context.getConfig("operation").getContent();
        operation.setChannelRatios(Collections.singletonMap("hot", 1.0));
        List<ScoreResult> input = scores("i2i-1", "i2i-2", "hot-1", "hot-2");
        input.get(0).setRecallFrom("i2i"); input.get(1).setRecallFrom("i2i");
        input.get(2).setRecallFrom("hot"); input.get(3).setRecallFrom("hot");
        context.addData("operationItemMap", items(item("i2i-1", "a", "x"),
            item("i2i-2", "b", "y"), item("hot-1", "c", "z"), item("hot-2", "d", "w")));

        assertEquals(Arrays.asList("i2i-1", "i2i-2", "hot-1", "hot-2"),
            ids(new SlidingWindowDiversityOperationRule().handle(context, input)));
    }

    private static GraphContext context(int size, int window, int repeat, String... dimensions) {
        OperationConfig operation = new OperationConfig();
        operation.setWindowSize(window); operation.setRepeatK(repeat);
        operation.setDiversityDimensions(Arrays.asList(dimensions));
        NodeConfig<OperationConfig> node = new NodeConfig<>(); node.setContent(operation);
        GraphContext context = new GraphContext(); context.addParam("size", size);
        context.addConfig("operation", node); return context;
    }

    private static List<ScoreResult> scores(String... ids) {
        List<ScoreResult> result = new ArrayList<>();
        for (int i = 0; i < ids.length; i++) { result.add(new ScoreResult(ids[i], ids.length - i)); }
        return result;
    }

    private static Item item(String id, String category, String tags) {
        Item item = new Item(); item.setId(id); item.setCategory(category); item.setTags(tags); return item;
    }

    private static Map<String, Item> items(Item... items) {
        Map<String, Item> result = new LinkedHashMap<>();
        for (Item item : items) { result.put(item.getId(), item); } return result;
    }

    private static List<String> ids(List<ScoreResult> items) {
        List<String> result = new ArrayList<>();
        for (ScoreResult item : items) { result.add(item.getId()); } return result;
    }

    private static void assertWindowLimit(List<ScoreResult> result, Map<String, Item> items,
        int window, int repeat, List<String> dimensions) {
        for (int start = 0; start + window <= result.size(); start++) {
            Map<String, Integer> counts = new LinkedHashMap<>();
            for (int i = start; i < start + window; i++) {
                for (String key : SlidingWindowDiversityOperationRule.keys(items.get(result.get(i).getId()), dimensions)) {
                    counts.put(key, counts.getOrDefault(key, 0) + 1);
                }
            }
            assertTrue(counts.values().stream().allMatch(count -> count <= repeat));
        }
    }
}
