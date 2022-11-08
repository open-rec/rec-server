package com.openrec.graph.node;

import com.google.common.collect.Maps;
import com.openrec.graph.GraphContext;
import com.openrec.graph.config.NodeConfig;
import com.openrec.graph.tools.anno.Export;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

import static com.openrec.graph.RecParams.USER_ID;

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
        // TODO: 2022/11/2 get user feature.
        userFeatureMap = Maps.newHashMap();
        log.info("{} with size:{}", getName(), userFeatureMap.size());
    }
}
