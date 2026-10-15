package com.openrec.graph.node;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import com.openrec.proto.model.ScoreResult;

/** Isolates rank writes from recall candidates retained for timeout fallback. */
public final class RankCandidates {
    private RankCandidates() {}

    public static List<ScoreResult> copy(List<ScoreResult> candidates, int limit) {
        List<ScoreResult> result = new ArrayList<>();
        for (ScoreResult source : candidates.subList(0, Math.min(limit, candidates.size()))) {
            ScoreResult copy = new ScoreResult(source.getId(), source.getScore(), source.getRecallFrom());
            copy.setRecallScore(source.getRecallScore());
            copy.setRecallFusionScore(source.getRecallFusionScore());
            copy.setRankScore(source.getRankScore());
            if (source.getRecallScores() != null)
                copy.setRecallScores(new LinkedHashMap<>(source.getRecallScores()));
            result.add(copy);
        }
        return result;
    }
}
