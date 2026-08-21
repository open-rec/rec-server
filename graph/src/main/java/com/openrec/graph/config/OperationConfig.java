package com.openrec.graph.config;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import lombok.Data;

@Data
public class OperationConfig {

    private String operationName;

    /** Target distribution used by WeightedChannelOperationRule. */
    private Map<String, Double> channelRatios = new LinkedHashMap<>();

    /** Guaranteed minimum distribution used by RandomInsertOperationRule. */
    private Map<String, Double> randomInsertRatios = new LinkedHashMap<>();

    /** Sliding window length used by SlidingWindowDiversityOperationRule. */
    private int windowSize;

    /** Maximum occurrences of the same diversity key in one window. */
    private int repeatK;

    /** category, tags, or composite expressions such as category+tags. */
    private List<String> diversityDimensions = new ArrayList<>();
}
