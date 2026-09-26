package com.openrec.graph.config;

import lombok.Data;

/** Configuration for request-text sparse retrieval. */
@Data
public class SparseConfig extends RecallConfig {

    private int size;
    private String queryParam = "query";
    private String textField = "text";
    private String minimumShouldMatch;
}
