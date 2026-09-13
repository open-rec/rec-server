package com.openrec.graph.node.user;

import com.openrec.graph.node.AbstractRecallNode;

import static com.openrec.graph.RecParams.SCENE;
import static com.openrec.graph.RecParams.USER_ID;

import java.util.List;
import com.openrec.graph.GraphContext;
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
        List<ScoreResult> users = recallStore.u2u(tableName(), context.getParams().getValueToString(SCENE),
            context.getParams().getValueToString(USER_ID), config.getContent().getSize());
        exportChannel(context, users);
    }
}
