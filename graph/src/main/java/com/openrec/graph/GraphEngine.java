package com.openrec.graph;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.openrec.graph.config.NodeConfig;
import com.openrec.graph.node.Node;
import com.openrec.graph.node.RootNode;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
public class GraphEngine {

    private ExecutorService threadPool;
    private List<Node> queue;

    public GraphEngine() {
        this.threadPool = Executors.newCachedThreadPool();
        this.queue = Lists.newLinkedList();
    }


    public void buildGraph(GraphConfig graphConfig) {
        RootNode rootNode = new RootNode();
        Map<String, Node> nodeMap = Maps.newHashMap();
        for(NodeConfig nodeConfig: graphConfig.getNodes()) {
            if(StringUtils.isNotEmpty(nodeConfig.getClazz())) {
                try {
                    Node node = (Node) Class.forName(nodeConfig.getClazz())
                            .getDeclaredConstructor(NodeConfig.class)
                            .newInstance(nodeConfig);
                    nodeMap.put(node.getName(), node);
                    if(node.isReady()) {
                        rootNode.addChild(node);
                    }
                } catch (Exception e) {
                    log.error("node:{} reflect failed: {}", nodeConfig.getClazz(), ExceptionUtils.getStackTrace(e));
                }
            }
        }

        for(GraphConfig.NodeEdge edge : graphConfig.getEdges()) {
            Node from = nodeMap.get(edge.getFrom());
            Node to = nodeMap.get(edge.getTo());
            from.addChild(to);
            to.addParent(from);
        }

        queue.add(rootNode);
        log.info("build graph finished");
    }

    public void execGraph() {

    }

    public void destroy() {
        this.threadPool.shutdownNow();
        this.queue.clear();
    }
}
