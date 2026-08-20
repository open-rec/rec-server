package com.openrec.service.recall;

import java.util.List;

import com.openrec.proto.model.ScoreResult;

/** Storage-neutral access to the four offline recall channels. */
public interface RecallStore {

    List<ScoreResult> hot(String scene, int size);

    /** Returns normalized freshness scores for items published inside the requested Unix-time window. */
    List<ScoreResult> newest(String scene, long startTime, long endTime, int size);

    /** Merges candidates from all triggers, summing scores for duplicate candidates. */
    List<ScoreResult> i2i(String scene, List<String> triggerItems, int size);

    List<ScoreResult> embedding(String scene, List<String> triggerItems, int size, long timeoutMillis);
}
