package com.openrec.graph.node;

import static com.openrec.graph.RecParams.USER_ID;

import java.util.Map;

import com.google.common.collect.Maps;
import com.openrec.graph.GraphContext;
import com.openrec.graph.config.NodeConfig;
import com.openrec.graph.tools.anno.Export;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class UserFeatureNode extends SyncNode<Void> {

    @Export("userFeatureMap")
    private Map<String, String> userFeatureMap;

    public UserFeatureNode(NodeConfig nodeConfig) {
        super(nodeConfig);
        this.userFeatureMap = Maps.newHashMap();
    }

    @Override
    public void run(GraphContext context) {
        String userId = context.getParams().getValueToString(USER_ID);
        userFeatureMap = Maps.newHashMap();
        userFeatureMap.put(USER_ID, userId);
        log.info("{} with size:{}", getName(), userFeatureMap.size());
    }
}
