package com.openrec.graph;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.*;

import org.apache.commons.lang3.exception.ExceptionUtils;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.openrec.graph.config.NodeConfig;
import com.openrec.graph.node.Node;
import com.openrec.graph.node.RootNode;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class GraphEngine {

    private static ExecutorService threadPool =
        new ThreadPoolExecutor(Runtime.getRuntime().availableProcessors() * 2, Integer.MAX_VALUE, 60L, TimeUnit.SECONDS,
            new SynchronousQueue<>(), new ThreadFactoryBuilder().setNameFormat("graph-engine-pool").build());

    private static ExecutorService timeoutThreadPool =
        new ThreadPoolExecutor(Runtime.getRuntime().availableProcessors(), Integer.MAX_VALUE, 60L, TimeUnit.SECONDS,
            new SynchronousQueue<>(), new ThreadFactoryBuilder().setNameFormat("graph-timeout-pool").build());

    private GraphContext context;
    private Queue<Node> queue;
    private Set<String> nodeSet;

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

    public void addParam(String key, Object value) {
        context.addParam(key, value);
    }

    public void buildGraph(GraphConfig graphConfig) {
        try {
            buildGraph(GraphPlan.compile(graphConfig));
        } catch (IllegalArgumentException error) {
            log.error("compile graph failed: {}", ExceptionUtils.getStackTrace(error));
            queue.add(new RootNode());
        }
    }

    public void buildGraph(GraphPlan plan) {
        RootNode rootNode = new RootNode();
        Node[] nodes = new Node[plan.size()];
        for (int index = 0; index < nodes.length; index++) {
            NodeConfig nodeConfig = plan.getNodeConfig(index);
            context.addConfig(nodeConfig.getName(), nodeConfig);
            nodes[index] = plan.newNode(index);
        }

        for (int[] edge : plan.getEdges()) {
            Node from = nodes[edge[0]];
            Node to = nodes[edge[1]];
            from.addChild(to);
            to.addParent(from);
        }
        for (int root : plan.getRoots()) rootNode.addChild(nodes[root]);

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
                            log.info("node:{} exec cost time: {}ms", node.getName(),
                                System.currentTimeMillis() - start);
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

    /** Executes a precompiled plan without rebuilding Node parent/child relationships. */
    public void execGraph(GraphPlan plan) {
        Node[] nodes = new Node[plan.size()];
        for (int index = 0; index < nodes.length; index++) {
            NodeConfig config = plan.getNodeConfig(index);
            context.addConfig(config.getName(), config);
            nodes[index] = plan.newNode(index);
        }
        int[] dependencies = plan.newDependencyCounts();
        List<Integer> ready = new ArrayList<>();
        for (int root : plan.getRoots()) ready.add(root);
        int executed = 0;
        while (!ready.isEmpty()) {
            CountDownLatch latch = new CountDownLatch(ready.size());
            for (int index : ready) {
                Node node = nodes[index];
                node.start();
                Future future = threadPool.submit(() -> {
                    long start = System.currentTimeMillis();
                    try {
                        context.importNodeData(node);
                        node.run(context);
                        context.exportNodeData(node);
                    } catch (Exception error) {
                        log.error("node:{} exec with exception:{}", node.getName(), ExceptionUtils.getStackTrace(error));
                    } finally {
                        node.stop();
                        latch.countDown();
                        log.info("node:{} exec cost time: {}ms", node.getName(), System.currentTimeMillis() - start);
                    }
                });
                timeoutThreadPool.submit(new TimeoutTask(node.getName(), future, node.getTimeout()));
            }
            try {
                latch.await();
            } catch (InterruptedException error) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(error);
            }
            List<Integer> next = new ArrayList<>();
            for (int index : ready) {
                executed++;
                for (int child : plan.getChildren(index)) {
                    if (--dependencies[child] == 0) next.add(child);
                }
            }
            ready = next;
        }
        if (executed != nodes.length) throw new IllegalStateException("compiled graph contains unreachable nodes");
    }

    public <T> T getResult() {
        return (T)context.getResult();
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
}
