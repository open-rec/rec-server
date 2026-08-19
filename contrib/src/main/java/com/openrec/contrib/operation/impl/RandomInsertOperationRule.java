package com.openrec.contrib.operation.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import org.pf4j.Extension;

import com.openrec.contrib.operation.OperationRule;
import com.openrec.graph.GraphContext;
import com.openrec.graph.config.OperationConfig;
import com.openrec.proto.model.ScoreResult;

/** Reserves channel candidates and inserts them at random positions in the final result. */
@Extension
public class RandomInsertOperationRule implements OperationRule {

    private final Random random;

    public RandomInsertOperationRule() {
        this(new Random());
    }

    RandomInsertOperationRule(Random random) {
        this.random = random;
    }

    @Override
    public List<ScoreResult> handle(GraphContext context, List<ScoreResult> inputItems) {
        if (inputItems == null || inputItems.isEmpty()) {
            return Collections.emptyList();
        }
        OperationConfig config = ChannelMixSupport.config(context);
        if (config.getRandomInsertRatios() == null || config.getRandomInsertRatios().isEmpty()) {
            return inputItems;
        }
        int size = ChannelMixSupport.resultSize(context, inputItems);
        Map<String, Integer> quotas =
            ChannelMixSupport.minimumQuotas(config.getRandomInsertRatios(), size);
        List<ScoreResult> reserved = ChannelMixSupport.selectReserved(
            quotas, ChannelMixSupport.buckets(inputItems));

        Set<String> reservedIds = reserved.stream().map(ScoreResult::getId).collect(Collectors.toSet());
        Set<String> configuredChannels = config.getRandomInsertRatios().keySet();
        List<ScoreResult> base = ChannelMixSupport.sorted(inputItems).stream()
            .filter(item -> !reservedIds.contains(item.getId()))
            .filter(item -> !configuredChannels.contains(item.getRecallFrom()))
            .limit(Math.max(size - reserved.size(), 0))
            .collect(Collectors.toCollection(ArrayList::new));
        // Keep the configured share exact when other channels can fill the result. Only exceed it
        // when the non-configured channels are themselves too short.
        if (base.size() + reserved.size() < size) {
            Set<String> selectedIds = base.stream().map(ScoreResult::getId).collect(Collectors.toSet());
            selectedIds.addAll(reservedIds);
            for (ScoreResult item : ChannelMixSupport.sorted(inputItems)) {
                if (base.size() + reserved.size() >= size) {
                    break;
                }
                if (selectedIds.add(item.getId())) {
                    base.add(item);
                }
            }
        }

        Collections.shuffle(reserved, random);
        Set<Integer> positions = new LinkedHashSet<>();
        while (positions.size() < reserved.size()) {
            positions.add(random.nextInt(size));
        }
        List<ScoreResult> result = new ArrayList<>(size);
        int reservedIndex = 0;
        int baseIndex = 0;
        for (int position = 0; position < size; position++) {
            if (positions.contains(position) && reservedIndex < reserved.size()) {
                result.add(reserved.get(reservedIndex++));
            } else if (baseIndex < base.size()) {
                result.add(base.get(baseIndex++));
            }
        }
        return result;
    }
}
