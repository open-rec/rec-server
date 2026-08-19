package com.openrec.contrib.operation.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.openrec.graph.GraphContext;
import com.openrec.graph.config.NodeConfig;
import com.openrec.graph.config.OperationConfig;
import com.openrec.proto.model.ScoreResult;

final class ChannelMixSupport {

    private static final Comparator<ScoreResult> SCORE_DESC =
        Comparator.comparingDouble(ScoreResult::getScore).reversed();

    private ChannelMixSupport() {}

    static OperationConfig config(GraphContext context) {
        NodeConfig<?> nodeConfig = context.getConfig("operation");
        if (nodeConfig == null || !(nodeConfig.getContent() instanceof OperationConfig)) {
            return new OperationConfig();
        }
        return (OperationConfig)nodeConfig.getContent();
    }

    static int resultSize(GraphContext context, List<ScoreResult> input) {
        int requested = context.getParams().getValueToInt("size");
        return Math.min(requested > 0 ? requested : input.size(), input.size());
    }

    static List<ScoreResult> sorted(List<ScoreResult> input) {
        List<ScoreResult> result = new ArrayList<>(input);
        result.sort(SCORE_DESC);
        return result;
    }

    static Map<String, List<ScoreResult>> buckets(List<ScoreResult> input) {
        Map<String, List<ScoreResult>> buckets = input.stream()
            .filter(item -> item.getRecallFrom() != null)
            .collect(Collectors.groupingBy(ScoreResult::getRecallFrom, LinkedHashMap::new, Collectors.toList()));
        buckets.values().forEach(items -> items.sort(SCORE_DESC));
        return buckets;
    }

    static Map<String, Integer> proportionalQuotas(Map<String, Double> ratios, int size) {
        Map<String, Double> valid = validRatios(ratios);
        if (valid.isEmpty() || size <= 0) {
            return Collections.emptyMap();
        }
        double total = valid.values().stream().mapToDouble(Double::doubleValue).sum();
        Map<String, Integer> quotas = new LinkedHashMap<>();
        Map<String, Double> remainders = new LinkedHashMap<>();
        int allocated = 0;
        for (Map.Entry<String, Double> entry : valid.entrySet()) {
            double exact = size * entry.getValue() / total;
            int quota = (int)Math.floor(exact);
            quotas.put(entry.getKey(), quota);
            remainders.put(entry.getKey(), exact - quota);
            allocated += quota;
        }
        List<String> remainderOrder = new ArrayList<>(valid.keySet());
        remainderOrder.sort((left, right) -> Double.compare(remainders.get(right), remainders.get(left)));
        for (int i = 0; allocated < size; i++, allocated++) {
            String channel = remainderOrder.get(i % remainderOrder.size());
            quotas.put(channel, quotas.get(channel) + 1);
        }
        return quotas;
    }

    static Map<String, Integer> minimumQuotas(Map<String, Double> ratios, int size) {
        Map<String, Double> valid = validRatios(ratios);
        Map<String, Integer> quotas = new LinkedHashMap<>();
        int allocated = 0;
        for (Map.Entry<String, Double> entry : valid.entrySet()) {
            int quota = (int)Math.ceil(size * entry.getValue());
            quota = Math.min(quota, Math.max(size - allocated, 0));
            quotas.put(entry.getKey(), quota);
            allocated += quota;
        }
        return quotas;
    }

    static List<ScoreResult> selectReserved(Map<String, Integer> quotas,
        Map<String, List<ScoreResult>> buckets) {
        List<ScoreResult> selected = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : quotas.entrySet()) {
            List<ScoreResult> bucket = buckets.getOrDefault(entry.getKey(), Collections.emptyList());
            selected.addAll(bucket.subList(0, Math.min(entry.getValue(), bucket.size())));
        }
        return selected;
    }

    static List<ScoreResult> fillHighest(List<ScoreResult> input, List<ScoreResult> selected, int size) {
        Set<String> selectedIds = selected.stream().map(ScoreResult::getId)
            .collect(Collectors.toCollection(LinkedHashSet::new));
        for (ScoreResult item : sorted(input)) {
            if (selected.size() >= size) {
                break;
            }
            if (selectedIds.add(item.getId())) {
                selected.add(item);
            }
        }
        return selected;
    }

    private static Map<String, Double> validRatios(Map<String, Double> ratios) {
        if (ratios == null) {
            return Collections.emptyMap();
        }
        Map<String, Double> valid = new LinkedHashMap<>();
        ratios.forEach((channel, ratio) -> {
            if (channel != null && ratio != null && ratio > 0d) {
                valid.put(channel, ratio);
            }
        });
        return valid;
    }
}
