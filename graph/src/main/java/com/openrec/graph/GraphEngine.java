package com.openrec.graph;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.openrec.graph.config.NodeConfig;
import com.openrec.graph.node.Node;
import com.openrec.graph.node.RootNode;
import com.openrec.graph.tools.anno.Export;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
public class GraphEngine {

    private ExecutorService threadPool;
    private GraphContext context;
    private Queue<Node> queue;
    private Set<String> nodeSet;

    public GraphEngine() {
        this.threadPool = Executors.newCachedThreadPool();
        this.queue = Lists.newLinkedList();
        this.nodeSet = Sets.newHashSet();
        this.context = new GraphContext();
    }

    public void prepare() {
        // TODO: 2022/10/31  init graph context
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

            if(from.isReady()) {
                rootNode.addChild(from);
            }
        }

        queue.add(rootNode);
        log.info("build graph finished");
    }

    public void execGraph() {
        while(!queue.isEmpty()) {
            Iterator<Node> iterator = queue.iterator();
            List<Node> readyNodes = Lists.newLinkedList();
            List<Node> nextNodes = Lists.newLinkedList();
            while(iterator.hasNext()) {
                Node curNode = iterator.next();
                if(curNode == null){
                    continue;
                }
                if(curNode.finished()) {
                    iterator.remove();
                    for(Node nextNode : curNode.getChildren()) {
                        if(nextNode.isReady() && !nodeSet.contains(nextNode.getName())) {
                            nextNodes.add(nextNode);
                            nodeSet.add(nextNode.getName());
                        }
                    }
                    curNode.destroy();
                } else if(curNode.isReady()) {
                    readyNodes.add(curNode);
                }
            }
            queue.addAll(nextNodes);

            int batch = readyNodes.size();
            if(batch>0) {
                CountDownLatch latch = new CountDownLatch(batch);
                for(Node node : readyNodes) {
                    node.start();
                    threadPool.submit(()->{
                        context.importNodeData(node);
                        node.run(context);
                        context.exportNodeData(node);
                        node.stop();
                        latch.countDown();
                    });
                }
                try {
                    latch.await();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        log.info("graph execute finished, total node count:{}", nodeSet.size());
    }

    public void destroy() {
        this.threadPool.shutdownNow();
        this.queue.clear();
        this.context.clean();
    }
}
