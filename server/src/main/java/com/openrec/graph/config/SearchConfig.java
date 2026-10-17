package com.openrec.graph.config;

import lombok.Data;

/** Configuration for request-text full-text retrieval. */
@Data
public class SearchConfig extends RecallConfig {

    private int size;
    private String queryParam = "query";
    private String textField = "text";
    private String minimumShouldMatch;
}
