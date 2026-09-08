package com.openrec.graph.node;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import com.openrec.graph.GraphContext;
import com.openrec.graph.config.CombineConfig;
import com.openrec.graph.config.NodeConfig;
import com.openrec.proto.model.ScoreResult;

/** Shared channel attribution, exclusion and de-duplication for entity recommendation graphs. */
public abstract class AbstractCombineNode extends SyncNode<CombineConfig> {
    protected AbstractCombineNode(NodeConfig nodeConfig) { super(nodeConfig); }

    protected MergeResult mergeChannels(GraphContext context,
        Map<String, List<ScoreResult>> fallbackChannels, Set<String> filtered,
        Set<String> blacklisted, Set<String> triggers) {
        Map<String, ScoreResult> candidates = new LinkedHashMap<>();
        int[] counters = new int[3];
        List<String> recallTypes = config.getContent().getRecallTypes();
        if (recallTypes == null || recallTypes.isEmpty()) {
            fallbackChannels.forEach((channel, values) ->
                collect(values, channel, candidates, filtered, blacklisted, triggers, counters));
        } else {
            for (String channel : recallTypes) {
                @SuppressWarnings("unchecked") List<ScoreResult> values =
                    (List<ScoreResult>) context.getData(RecallNode.CHANNEL_PREFIX + channel);
                collect(values, channel, candidates, filtered, blacklisted, triggers, counters);
            }
        }
        return new MergeResult(candidates, counters);
    }

    protected MergeResult mergeChannels(GraphContext context, Set<String> triggers) {
        return mergeChannels(context, Collections.emptyMap(), Collections.emptySet(),
            Collections.emptySet(), triggers);
    }

    /** The item graph retains the first score; user graphs can override score fusion. */
    protected void mergeScore(ScoreResult existing, ScoreResult incoming) { }

    private void collect(List<ScoreResult> values, String channel, Map<String, ScoreResult> candidates,
        Set<String> filtered, Set<String> blacklisted, Set<String> triggers, int[] counters) {
        if (values == null) return;
        for (ScoreResult value : values) {
            String id = value.getId();
            if (id == null) continue;
            if (filtered != null && filtered.contains(id)) { counters[0]++; continue; }
            if (blacklisted != null && blacklisted.contains(id)) { counters[1]++; continue; }
            if (triggers != null && triggers.contains(id)) { counters[2]++; continue; }
            ScoreResult existing = candidates.get(id);
            if (existing == null) {
                value.addRecallScore(channel, value.getScore()); candidates.put(id, value);
            } else {
                existing.addRecallScore(channel, value.getScore()); mergeScore(existing, value);
            }
        }
    }

    protected static final class MergeResult {
        private final Map<String, ScoreResult> candidates; private final int[] counters;
        private MergeResult(Map<String, ScoreResult> candidates, int[] counters) {
            this.candidates = candidates; this.counters = counters;
        }
        protected Map<String, ScoreResult> candidates() { return candidates; }
        protected int filtered() { return counters[0]; }
        protected int blacklisted() { return counters[1]; }
        protected int triggered() { return counters[2]; }
    }
}
