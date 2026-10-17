package com.openrec.graph.node.user;

import com.openrec.graph.node.AbstractRecallNode;

import static com.openrec.graph.RecParams.SCENE;

import java.util.List;
import com.openrec.graph.GraphContext;
import com.openrec.graph.RequestTrigger;
import com.openrec.graph.config.NodeConfig;
import com.openrec.graph.config.U2uConfig;
import com.openrec.proto.model.ScoreResult;
import com.openrec.service.recall.RecallStore;
import org.springframework.beans.factory.annotation.Autowired;

/** Reads one behaviour- or profile-based user similarity channel. */
public class U2uNode extends AbstractRecallNode<U2uConfig> {
    @Autowired
    private RecallStore recallStore;

    public U2uNode(NodeConfig nodeConfig) {
        super(nodeConfig);
    }

    @Override
    public void run(GraphContext context) {
        if (!config.isOpen())
            return;
        java.util.Map<String, Double> merged = new java.util.LinkedHashMap<>();
        for (String seed : RequestTrigger.resolve(context).userSeedIds()) {
            for (ScoreResult candidate : recallStore.u2u(tableName(), context.getParams().getValueToString(SCENE), seed,
                config.getContent().getSize())) {
                merged.merge(candidate.getId(), candidate.getScore(), Double::sum);
            }
        }
        List<ScoreResult> users = merged.entrySet().stream()
            .map(entry -> new ScoreResult(entry.getKey(), entry.getValue())).sorted(java.util.Comparator
                .comparingDouble(ScoreResult::getScore).reversed().thenComparing(ScoreResult::getId))
            .limit(config.getContent().getSize()).toList();
        exportChannel(context, users);
    }
}
