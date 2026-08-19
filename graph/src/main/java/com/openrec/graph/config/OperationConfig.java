package com.openrec.graph.config;

import java.util.LinkedHashMap;
import java.util.Map;

import lombok.Data;

@Data
public class OperationConfig {

    private String operationName;

    /** Target distribution used by WeightedChannelOperationRule. */
    private Map<String, Double> channelRatios = new LinkedHashMap<>();

    /** Guaranteed minimum distribution used by RandomInsertOperationRule. */
    private Map<String, Double> randomInsertRatios = new LinkedHashMap<>();
}
