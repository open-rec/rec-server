package com.openrec.graph;

import java.util.List;

import com.openrec.graph.config.NodeConfig;

import lombok.Data;

@Data
public class GraphConfig {

    private List<NodeConfig> nodes;
    private List<NodeEdge> edges;

    @Data
    static class NodeEdge {
        private String from;
        private String to;
    }
}
