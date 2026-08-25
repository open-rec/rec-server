package com.openrec.service.recall;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

/** Source schema shared by the versioned offline recall indexes. */
@Data
public class RecallDocument {
    private String scene;
    /** Item identifier used by the legacy embedding index. */
    private String id;
    private String item;
    private String user;

    @JsonProperty("left_item")
    private String leftItem;

    @JsonProperty("right_item")
    private String rightItem;

    private Double score;

    @JsonProperty("publish_time")
    private Long publishTime;

    private List<Double> vector;
}
