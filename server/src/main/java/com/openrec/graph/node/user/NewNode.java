package com.openrec.graph.node.user;

import static com.openrec.graph.RecParams.SCENE;

import java.util.ArrayList;
import java.util.List;

import com.openrec.graph.GraphContext;
import com.openrec.graph.config.NewConfig;
import com.openrec.graph.config.NodeConfig;
import com.openrec.graph.node.AbstractRecallNode;
import com.openrec.graph.tools.anno.Export;
import com.openrec.proto.model.ScoreResult;
import com.openrec.service.recall.RecallStore;
import com.openrec.util.BeanUtil;
import com.openrec.util.TimeUtil;

public class NewNode extends AbstractRecallNode<NewConfig> {
    private RecallStore recallStore = BeanUtil.getBean(RecallStore.class);
    @Export("newUsers")
    private List<ScoreResult> newUsers = new ArrayList<>();

    public NewNode(NodeConfig nodeConfig) {
        super(nodeConfig);
    }

    @Override
    public void run(GraphContext context) {
        if (!config.isOpen())
            return;
        long now = TimeUtil.nowSecs();
        newUsers = recallStore.newestUsers(tableName(), context.getParams().getValueToString(SCENE),
            now - config.getContent().getDuration(), now, config.getContent().getSize());
        exportChannel(context, newUsers);
    }
}
