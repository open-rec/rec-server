package com.openrec.proto.model;

import java.io.Serializable;

import lombok.Data;

@Data
public class ScoreResult implements Serializable {

    private String id;

    /** What the client ranks by: recallScore + rankScore once both stages have run. */
    private double score;

    /**
     * Which recall channel produced this item — i2i, embedding, hot or new. Set by CombineNode; an
     * item surfaced by several channels keeps the first one that produced it.
     */
    private String recallFrom;

    /**
     * The score as it came out of recall, kept separate so the two stages can be told apart
     * downstream. Boxed deliberately: null means the stage did not run, which is not the same as a
     * score of 0.
     */
    private Double recallScore;

    /** What the rank engine contributed. null when ranking was skipped or failed. */
    private Double rankScore;

    public ScoreResult() {}

    public ScoreResult(String id, double score) {
        this.id = id;
        this.score = score;
    }

    public ScoreResult(String id, double score, String recallFrom) {
        this(id, score);
        this.recallFrom = recallFrom;
    }
}
