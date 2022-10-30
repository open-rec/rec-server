package com.openrec.graph;

import com.google.common.collect.Maps;
import com.openrec.graph.config.NodeConfig;

import java.util.Map;

public class GraphContext {

    private GraphParams params;
    private Map<String, NodeConfig> configMap;

    public GraphContext() {
        this.params = new GraphParams();
        this.configMap = Maps.newHashMap();
    }

    public void addParam(String key, Object value) {
        params.put(key, value);
    }

    public void addConfig(String key, NodeConfig nodeConfig) {
        configMap.put(key, nodeConfig);
    }

    public void clean(){
        params.clear();
        configMap.clear();
    }
}
