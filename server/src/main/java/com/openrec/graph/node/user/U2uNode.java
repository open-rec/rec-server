package com.openrec.graph.node.user;

import com.openrec.graph.node.RecallNode;

import static com.openrec.graph.RecParams.SCENE;
import static com.openrec.graph.RecParams.USER_ID;

import java.util.List;
import com.openrec.graph.GraphContext;
import com.openrec.graph.config.NodeConfig;
import com.openrec.graph.config.U2uConfig;
import com.openrec.proto.model.ScoreResult;
import com.openrec.service.recall.RecallStore;
import com.openrec.util.BeanUtil;

/** Reads one behaviour- or profile-based user similarity channel. */
public class U2uNode extends RecallNode<U2uConfig> {
    private RecallStore recallStore = BeanUtil.getBean(RecallStore.class);
    public U2uNode(NodeConfig nodeConfig) { super(nodeConfig); }
    @Override public void run(GraphContext context) {
        if (!config.isOpen()) return;
        List<ScoreResult> users = recallStore.u2u(tableName(),
            context.getParams().getValueToString(SCENE), context.getParams().getValueToString(USER_ID),
            config.getContent().getSize());
        exportChannel(context, users);
    }
}
