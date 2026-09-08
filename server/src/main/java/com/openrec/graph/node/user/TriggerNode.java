package com.openrec.graph.node.user;

import static com.openrec.graph.RecParams.USER_ID;

import java.util.Collections;
import java.util.List;

import com.openrec.graph.GraphContext;
import com.openrec.graph.config.NodeConfig;
import com.openrec.graph.config.UserTriggerConfig;
import com.openrec.graph.node.AbstractSyncNode;
import com.openrec.graph.tools.anno.Export;
import com.openrec.proto.model.ScoreResult;

/** Supplies the requesting user as the seed for user-to-user recall. */
public class TriggerNode extends AbstractSyncNode<UserTriggerConfig> {
    @Export("triggerUsers")
    private List<ScoreResult> triggerUsers = Collections.emptyList();

    public TriggerNode(NodeConfig nodeConfig) {
        super(nodeConfig);
    }

    @Override
    public void run(GraphContext context) {
        if (!config.isOpen())
            return;
        String userId = context.getParams().getValueToString(USER_ID);
        triggerUsers = userId == null || userId.trim().isEmpty() ? Collections.emptyList()
            : Collections.singletonList(new ScoreResult(userId, 1d));
    }
}
