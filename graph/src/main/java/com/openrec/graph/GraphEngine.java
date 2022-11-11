package com.openrec.graph;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.openrec.graph.config.NodeConfig;
import com.openrec.graph.node.Node;
import com.openrec.graph.node.RootNode;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.*;

@Slf4j
public class GraphEngine {

    private static ExecutorService threadPool = new ThreadPoolExecutor(
            Runtime.getRuntime().availableProcessors() * 2,
            Integer.MAX_VALUE,
            60L,
            TimeUnit.SECONDS,
            new SynchronousQueue<>(),
            new ThreadFactoryBuilder().setNameFormat("graph-engine-pool").build());

    private static ExecutorService timeoutThreadPool = new ThreadPoolExecutor(
            Runtime.getRuntime().availableProcessors(),
            Integer.MAX_VALUE,
            60L,
            TimeUnit.SECONDS,
            new SynchronousQueue<>(),
            new ThreadFactoryBuilder().setNameFormat("graph-timeout-pool").build());

    private GraphContext context;
    private Queue<Node> queue;
    private Set<String> nodeSet;


    class TimeoutTask implements Callable<Void> {
        private String name;
        private Future future;
        private int timeout;

        public TimeoutTask(String name, Future future, int timeout) {
            this.name = name;
            this.future = future;
            this.timeout = timeout;
        }


        @Override
        public Void call() throws Exception {
            if (future != null) {
                try {
                    future.get(timeout, TimeUnit.MILLISECONDS);
                } catch (Exception e) {
                    if (!future.isCancelled()) {
                        future.cancel(true);
                    }
                    log.error("graph node:{} exec timeout, canceled by engine", name);
                }
            }
            return null;
        }
    }


    private GraphEngine() {
        this.queue = Lists.newLinkedList();
        this.nodeSet = Sets.newHashSet();
        this.context = new GraphContext();
    }

    public static GraphEngine getSessionGraphEngine() {
        return new GraphEngine();
    }

    public void prepare(Object paramsObj) {
        if (paramsObj != null) {
            for (Field field : paramsObj.getClass().getDeclaredFields()) {
                try {
                    field.setAccessible(true);
                    context.addParam(field.getName(), field.get(paramsObj));
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    public void buildGraph(GraphConfig graphConfig) {
        RootNode rootNode = new RootNode();
        Map<String, Node> nodeMap = Maps.newHashMap();
        for (NodeConfig nodeConfig : graphConfig.getNodes()) {
            context.addConfig(nodeConfig.getName(), nodeConfig);
            if (StringUtils.isNotEmpty(nodeConfig.getClazz())) {
                try {
                    Node node = (Node) Class.forName(nodeConfig.getClazz())
                            .getDeclaredConstructor(NodeConfig.class)
                            .newInstance(nodeConfig);
                    node.setConfig(nodeConfig);
                    nodeMap.put(node.getName(), node);
                } catch (Exception e) {
                    log.error("node:{} reflect failed: {}", nodeConfig.getClazz(), ExceptionUtils.getStackTrace(e));
                }
            }
        }

        for (GraphConfig.NodeEdge edge : graphConfig.getEdges()) {
            Node from = nodeMap.get(edge.getFrom());
            Node to = nodeMap.get(edge.getTo());
            from.addChild(to);
            to.addParent(from);

            if (from.isReady()) {
                rootNode.addChild(from);
            }
        }

        queue.add(rootNode);
        log.info("build graph finished");
    }

    public void execGraph() {
        while (!queue.isEmpty()) {
            Iterator<Node> iterator = queue.iterator();
            List<Node> readyNodes = Lists.newLinkedList();
            List<Node> nextNodes = Lists.newLinkedList();
            while (iterator.hasNext()) {
                Node curNode = iterator.next();
                if (curNode == null) {
                    continue;
                }
                if (curNode.finished()) {
                    iterator.remove();
                    for (Node nextNode : curNode.getChildren()) {
                        if (nextNode.isReady() && !nodeSet.contains(nextNode.getName())) {
                            nextNodes.add(nextNode);
                            nodeSet.add(nextNode.getName());
                        }
                    }
                    curNode.destroy();
                } else if (curNode.isReady()) {
                    readyNodes.add(curNode);
                }
            }
            queue.addAll(nextNodes);

            int batch = readyNodes.size();
            if (batch > 0) {
                CountDownLatch latch = new CountDownLatch(batch);
                for (Node node : readyNodes) {
                    node.start();
                    Future future = threadPool.submit(() -> {
                        long start = System.currentTimeMillis();
                        try {
                            context.importNodeData(node);
                            node.run(context);
                            context.exportNodeData(node);
                        } catch (Exception e) {
                            log.error("node:{} exec with exception:{}", ExceptionUtils.getStackTrace(e));
                        } finally {
                            node.stop();
                            latch.countDown();
                            log.info("node:{} exec cost time: {}ms",
                                    node.getName(),
                                    System.currentTimeMillis() - start
                            );
                        }
                    });
                    timeoutThreadPool.submit(new TimeoutTask(node.getName(), future, node.getTimeout()));
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

    public <T> T getResult() {
        return (T) context.getResult();
    }

    public void refresh() {
        // TODO: 2022/11/3 reuse collections
    }

    public void destroy() {
        this.threadPool.shutdownNow();
        this.queue.clear();
        this.nodeSet.clear();
        this.context.clean();
    }
}
