package com.openrec.graph.node.user;

import static com.openrec.graph.RecParams.SCENE;

import java.util.ArrayList;
import java.util.List;

import com.openrec.graph.GraphContext;
import com.openrec.graph.config.HotConfig;
import com.openrec.graph.config.NodeConfig;
import com.openrec.graph.node.AbstractRecallNode;
import com.openrec.graph.tools.anno.Export;
import com.openrec.proto.model.ScoreResult;
import com.openrec.service.recall.RecallStore;
import com.openrec.util.BeanUtil;

public class HotNode extends AbstractRecallNode<HotConfig> {
    private RecallStore recallStore = BeanUtil.getBean(RecallStore.class);
    @Export("hotUsers")
    private List<ScoreResult> hotUsers = new ArrayList<>();

    public HotNode(NodeConfig nodeConfig) {
        super(nodeConfig);
    }

    @Override
    public void run(GraphContext context) {
        if (!config.isOpen())
            return;
        hotUsers = recallStore.hotUsers(tableName(), context.getParams().getValueToString(SCENE),
            config.getContent().getSize());
        exportChannel(context, hotUsers);
    }
}
