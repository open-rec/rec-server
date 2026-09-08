package com.openrec.graph.node.user;

import com.openrec.graph.node.AbstractCombineNode;

import static com.openrec.graph.RecParams.USER_ID;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import com.openrec.graph.GraphContext;
import com.openrec.graph.config.NodeConfig;
import com.openrec.graph.tools.anno.Export;
import com.openrec.proto.model.ScoreResult;

/** Combines independent user recall channels without applying item availability rules. */
public class UserCombineNode extends AbstractCombineNode {
    @Export("userCandidates") private List<ScoreResult> candidates = new ArrayList<>();
    public UserCombineNode(NodeConfig nodeConfig) { super(nodeConfig); }
    @Override public void run(GraphContext context) {
        String requester = context.getParams().getValueToString(USER_ID);
        MergeResult merged = mergeChannels(context, java.util.Collections.singleton(requester));
        candidates = new ArrayList<>(merged.candidates().values());
        candidates.sort(Comparator.comparingDouble(ScoreResult::getScore).reversed()
            .thenComparing(ScoreResult::getId));
        int size = Math.min(config.getContent().getSize(), candidates.size());
        candidates = new ArrayList<>(candidates.subList(0, size));
    }

    @Override protected void mergeScore(ScoreResult existing, ScoreResult incoming) {
        existing.setScore(existing.getScore() + incoming.getScore());
    }
}
