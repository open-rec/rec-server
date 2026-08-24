package com.openrec.graph.config;

import java.util.List;

import lombok.Data;

@Data
public class RankScoreStrategyConfig {

    /** Supported values: first, max and sum. */
    private String recallAggregation = "first";

    private double recallWeight = 1d;

    private double rankWeight = 1d;

    /** Ordered for first aggregation; each entry also supplies its channel weight. */
    private List<RankChannelWeightConfig> channels;
}
