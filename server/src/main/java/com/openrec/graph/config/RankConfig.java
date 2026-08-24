package com.openrec.graph.config;

import lombok.Data;

@Data
public class RankConfig {

    private int size;

    /** Optional recall/rank score fusion. null preserves the legacy first-recall-score + rank behavior. */
    private RankScoreStrategyConfig scoreStrategy;
}
