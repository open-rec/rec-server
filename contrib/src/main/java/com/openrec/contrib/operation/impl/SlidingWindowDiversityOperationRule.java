package com.openrec.contrib.operation.impl;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.pf4j.Extension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.openrec.contrib.operation.OperationRule;
import com.openrec.graph.GraphContext;
import com.openrec.graph.config.OperationConfig;
import com.openrec.proto.model.Item;
import com.openrec.proto.model.ScoreResult;

/** Stable greedy reordering that caps repeated item attributes in every sliding window. */
@Extension
public class SlidingWindowDiversityOperationRule implements OperationRule {
    private static final Logger LOG = LoggerFactory.getLogger(SlidingWindowDiversityOperationRule.class);
    private final WeightedChannelOperationRule channelRule = new WeightedChannelOperationRule();

    @Override
    public List<ScoreResult> handle(GraphContext context, List<ScoreResult> inputItems) {
        if (inputItems == null || inputItems.isEmpty()) { return Collections.emptyList(); }
        OperationConfig config = ChannelMixSupport.config(context);
        int targetSize = ChannelMixSupport.resultSize(context, inputItems);
        if (config.getWindowSize() <= 0 || config.getRepeatK() <= 0
            || config.getDiversityDimensions() == null || config.getDiversityDimensions().isEmpty()) {
            return new ArrayList<>(inputItems.subList(0, targetSize));
        }

        // Keep the existing channel allocation as the preferred order, but retain all remaining
        // candidates as diversity fallbacks when the preferred set cannot satisfy the window.
        List<ScoreResult> preferred = channelRule.handle(context, inputItems);
        List<ScoreResult> remaining = new ArrayList<>(preferred);
        Set<String> seenIds = new LinkedHashSet<>();
        for (ScoreResult item : preferred) { seenIds.add(item.getId()); }
        for (ScoreResult item : ChannelMixSupport.sorted(inputItems)) {
            if (seenIds.add(item.getId())) { remaining.add(item); }
        }

        Map<String, Item> itemMap = itemMap(context);
        List<ScoreResult> result = new ArrayList<>(targetSize);
        Deque<Set<String>> window = new ArrayDeque<>();
        Map<String, Integer> counts = new HashMap<>();
        while (result.size() < targetSize && !remaining.isEmpty()) {
            if (window.size() >= config.getWindowSize()) { remove(window.removeFirst(), counts); }
            int selected = firstEligible(remaining, itemMap, config, counts);
            if (selected < 0) { break; }
            ScoreResult item = remaining.remove(selected);
            Set<String> keys = keys(itemMap.get(item.getId()), config.getDiversityDimensions());
            result.add(item);
            window.addLast(keys);
            for (String key : keys) { counts.put(key, counts.getOrDefault(key, 0) + 1); }
        }
        LOG.info("sliding diversity window:{}, repeatK:{}, dimensions:{}, inputSize:{}, resultSize:{}",
            config.getWindowSize(), config.getRepeatK(), config.getDiversityDimensions(),
            inputItems.size(), result.size());
        return result;
    }

    private static int firstEligible(List<ScoreResult> items, Map<String, Item> itemMap,
        OperationConfig config, Map<String, Integer> counts) {
        for (int i = 0; i < items.size(); i++) {
            boolean eligible = true;
            for (String key : keys(itemMap.get(items.get(i).getId()), config.getDiversityDimensions())) {
                if (counts.getOrDefault(key, 0) >= config.getRepeatK()) { eligible = false; break; }
            }
            if (eligible) { return i; }
        }
        return -1;
    }

    static Set<String> keys(Item item, List<String> dimensions) {
        Set<String> result = new LinkedHashSet<>();
        if (item == null) { return result; }
        for (String expression : dimensions) {
            if (expression == null || expression.trim().isEmpty()) { continue; }
            String normalized = expression.trim().toLowerCase();
            List<String> combinations = Collections.singletonList("");
            boolean valid = true;
            for (String dimension : normalized.split("\\+")) {
                List<String> values = values(item, dimension.trim());
                if (values.isEmpty()) { valid = false; break; }
                List<String> expanded = new ArrayList<>();
                for (String prefix : combinations) {
                    for (String value : values) {
                        expanded.add(prefix.isEmpty() ? value : prefix + "+" + value);
                    }
                }
                combinations = expanded;
            }
            if (valid) {
                for (String value : combinations) { result.add(normalized + "=" + value); }
            }
        }
        return result;
    }

    private static List<String> values(Item item, String dimension) {
        if ("category".equals(dimension)) {
            return blank(item.getCategory()) ? Collections.emptyList()
                : Collections.singletonList(item.getCategory().trim());
        }
        if (!"tags".equals(dimension) || blank(item.getTags())) { return Collections.emptyList(); }
        List<String> tags = new ArrayList<>();
        for (String tag : item.getTags().split("[,|]")) {
            String normalized = tag.trim().replaceAll("^[\\[\\]\\\"']+|[\\[\\]\\\"']+$", "");
            if (!normalized.isEmpty()) { tags.add(normalized); }
        }
        return tags;
    }

    private static void remove(Set<String> keys, Map<String, Integer> counts) {
        for (String key : keys) {
            int next = counts.getOrDefault(key, 0) - 1;
            if (next <= 0) { counts.remove(key); } else { counts.put(key, next); }
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Item> itemMap(GraphContext context) {
        Object value = context.getData("operationItemMap");
        return value instanceof Map ? (Map<String, Item>)value : Collections.emptyMap();
    }

    private static boolean blank(String value) { return value == null || value.trim().isEmpty(); }
}
