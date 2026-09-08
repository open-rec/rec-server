package com.openrec.graph.node.user;

import static com.openrec.graph.RecParams.SCENE;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;

import com.openrec.graph.GraphContext;
import com.openrec.graph.config.EmbeddingConfig;
import com.openrec.graph.config.NodeConfig;
import com.openrec.graph.node.AbstractRecallNode;
import com.openrec.graph.tools.anno.Export;
import com.openrec.graph.tools.anno.Import;
import com.openrec.proto.model.ScoreResult;
import com.openrec.service.recall.RecallStore;
import com.openrec.util.BeanUtil;

public class EmbeddingNode extends AbstractRecallNode<EmbeddingConfig> {
    private RecallStore recallStore = BeanUtil.getBean(RecallStore.class);
    @Import("triggerUsers")
    private List<ScoreResult> triggerUsers;
    @Export("embeddingUsers")
    private List<ScoreResult> embeddingUsers = new ArrayList<>();

    public EmbeddingNode(NodeConfig nodeConfig) {
        super(nodeConfig);
    }

    @Override
    public void run(GraphContext context) {
        if (!config.isOpen())
            return;
        List<String> triggers = triggerUsers.stream().map(ScoreResult::getId).collect(Collectors.toList());
        Map<String, Double> merged = new LinkedHashMap<>();
        for (String trigger : triggers) {
            for (ScoreResult user : recallStore.u2u(tableName(), context.getParams().getValueToString(SCENE), trigger,
                config.getContent().getSize())) {
                merged.merge(user.getId(), user.getScore(), Double::sum);
            }
        }
        embeddingUsers = merged.entrySet().stream().map(entry -> new ScoreResult(entry.getKey(), entry.getValue()))
            .sorted(Comparator.comparingDouble(ScoreResult::getScore).reversed().thenComparing(ScoreResult::getId))
            .limit(config.getContent().getSize()).collect(Collectors.toList());
        exportChannel(context, embeddingUsers);
    }
}
