package com.openrec.graph;

import com.openrec.graph.config.NodeConfig;
import lombok.Data;

import java.util.List;

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
