package com.openrec.proto.model;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

import lombok.Data;

@Data
public class ScoreResult implements Serializable {

    private String id;

    /** What the client ranks by: recallScore + rankScore once both stages have run. */
    private double score;

    /**
     * The channel whose score is carried in {@link #recallScore} — the first one that produced this
     * item. See {@link #recallScores} for everything that produced it.
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

    /**
     * Every recall channel that surfaced this item, in the order they were merged, with the score
     * each one gave it.
     * <p>
     * An item found by several channels is emitted once, but dropping the losing channels' scores
     * hides exactly the information needed to tune the strategy — whether i2i and hot agree, or
     * whether a single channel is carrying the whole result. Insertion-ordered so the merge order
     * stays readable.
     */
    private Map<String, Double> recallScores;

    public ScoreResult() {}

    public ScoreResult(String id, double score) {
        this.id = id;
        this.score = score;
    }

    public ScoreResult(String id, double score, String recallFrom) {
        this(id, score);
        this.recallFrom = recallFrom;
    }

    /** Records one channel's contribution; the first one also becomes {@link #recallFrom}. */
    public void addRecallScore(String channel, double channelScore) {
        if (recallScores == null) {
            recallScores = new LinkedHashMap<>();
        }
        recallScores.put(channel, channelScore);
        if (recallFrom == null) {
            recallFrom = channel;
        }
    }
}
