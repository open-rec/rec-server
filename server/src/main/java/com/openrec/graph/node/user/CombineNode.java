package com.openrec.graph.node.user;

import com.openrec.graph.node.AbstractCombineNode;

import static com.openrec.graph.RecParams.USER_ID;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import com.openrec.graph.GraphContext;
import com.openrec.graph.config.NodeConfig;
import com.openrec.graph.tools.anno.Export;
import com.openrec.graph.tools.anno.Import;
import com.openrec.proto.model.ScoreResult;

/** Combines independent user recall channels without applying item availability rules. */
public class CombineNode extends AbstractCombineNode {
    @Import("filterUserSet")
    private Set<String> filterUserSet = java.util.Collections.emptySet();
    @Import("blackUserSet")
    private Set<String> blackUserSet = java.util.Collections.emptySet();
    @Import("triggerUsers")
    private List<ScoreResult> triggerUsers = java.util.Collections.emptyList();
    @Export("userCandidates")
    private List<ScoreResult> candidates = new ArrayList<>();

    public CombineNode(NodeConfig nodeConfig) {
        super(nodeConfig);
    }

    @Override
    public void run(GraphContext context) {
        String requester = context.getParams().getValueToString(USER_ID);
        Set<String> triggers = triggerUsers == null ? java.util.Collections.emptySet()
            : triggerUsers.stream().map(ScoreResult::getId).collect(java.util.stream.Collectors.toSet());
        Set<String> excluded = new java.util.HashSet<>(triggers);
        excluded.add(requester);
        MergeResult merged =
            mergeChannels(context, java.util.Collections.emptyMap(), filterUserSet, blackUserSet, excluded);
        candidates = new ArrayList<>(merged.candidates().values());
        candidates.sort(Comparator.comparingDouble(ScoreResult::getScore).reversed().thenComparing(ScoreResult::getId));
        int size = Math.min(config.getContent().getSize(), candidates.size());
        candidates = new ArrayList<>(candidates.subList(0, size));
    }

    @Override
    protected void mergeScore(ScoreResult existing, ScoreResult incoming) {
        existing.setScore(existing.getScore() + incoming.getScore());
    }
}
