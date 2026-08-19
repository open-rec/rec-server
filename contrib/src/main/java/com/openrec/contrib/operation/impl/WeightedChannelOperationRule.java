package com.openrec.contrib.operation.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.pf4j.Extension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.openrec.contrib.operation.OperationRule;
import com.openrec.graph.GraphContext;
import com.openrec.graph.config.OperationConfig;
import com.openrec.proto.model.ScoreResult;

/** Selects the highest-scoring candidates while enforcing a configured channel distribution. */
@Extension
public class WeightedChannelOperationRule implements OperationRule {

    private static final Logger LOG = LoggerFactory.getLogger(WeightedChannelOperationRule.class);

    @Override
    public List<ScoreResult> handle(GraphContext context, List<ScoreResult> inputItems) {
        if (inputItems == null || inputItems.isEmpty()) {
            return Collections.emptyList();
        }
        OperationConfig config = ChannelMixSupport.config(context);
        if (config.getChannelRatios() == null || config.getChannelRatios().isEmpty()) {
            return inputItems;
        }
        int size = ChannelMixSupport.resultSize(context, inputItems);
        Map<String, Integer> quotas =
            ChannelMixSupport.proportionalQuotas(config.getChannelRatios(), size);
        Map<String, List<ScoreResult>> buckets = ChannelMixSupport.buckets(inputItems);
        Map<String, Integer> shortages = ChannelMixSupport.quotaShortages(quotas, buckets);
        List<ScoreResult> selected = ChannelMixSupport.selectReserved(quotas, buckets);
        ChannelMixSupport.fillHighest(inputItems, selected, size);
        selected.sort((left, right) -> Double.compare(right.getScore(), left.getScore()));
        LOG.info("weighted channel allocation target:{}, actual:{}, shortage:{}, inputSize:{}, resultSize:{}",
            quotas, ChannelMixSupport.channelCounts(selected), shortages, inputItems.size(), selected.size());
        return new ArrayList<>(selected);
    }
}
