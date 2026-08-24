package com.openrec.graph.config;

import lombok.Data;

@Data
public class RankChannelWeightConfig {

    private String name;

    private double weight = 1d;

    public RankChannelWeightConfig() {}

    public RankChannelWeightConfig(String name, double weight) {
        this.name = name;
        this.weight = weight;
    }
}
