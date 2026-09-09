package com.openrec.graph.node.user;

import static com.openrec.graph.RecParams.USER_ID;

import java.util.LinkedHashMap;
import java.util.Map;

import com.openrec.graph.GraphContext;
import com.openrec.graph.config.NodeConfig;
import com.openrec.graph.node.AbstractSyncNode;
import com.openrec.graph.tools.anno.Export;

/** Exposes request-user features consumed by user ranking. */
public class UserFeatureNode extends AbstractSyncNode<Void> {
    @Export("userFeatureMap")
    private Map<String, String> userFeatureMap = new LinkedHashMap<>();

    public UserFeatureNode(NodeConfig nodeConfig) {
        super(nodeConfig);
    }

    @Override
    public void run(GraphContext context) {
        userFeatureMap = new LinkedHashMap<>();
        userFeatureMap.put(USER_ID, context.getParams().getValueToString(USER_ID));
    }
}
