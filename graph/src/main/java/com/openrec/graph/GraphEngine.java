package com.openrec.graph;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.exception.ExceptionUtils;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.openrec.graph.config.NodeConfig;
import com.openrec.graph.node.FailurePolicy;
import com.openrec.graph.node.Node;
import com.openrec.graph.node.NodeStatus;
import com.openrec.graph.node.TypedNode;
import com.openrec.graph.data.NodeInput;
import com.openrec.graph.data.NodeOutput;
import com.openrec.graph.trace.GraphExecutionTrace;
import com.openrec.graph.trace.GraphTraceContext;
import com.openrec.graph.trace.GraphTraceObserver;
import com.openrec.graph.trace.NodeExecutionTrace;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class GraphEngine {

    private static final int WORKER_THREADS =
        Integer.getInteger("openrec.graph.worker.threads", Math.max(8, Runtime.getRuntime().availableProcessors() * 4));
    private static final int WORKER_QUEUE_CAPACITY = Integer.getInteger("openrec.graph.worker.queue-capacity", 1024);

    private static final ThreadPoolExecutor threadPool = new ThreadPoolExecutor(WORKER_THREADS, WORKER_THREADS, 0L,
        TimeUnit.MILLISECONDS, new ArrayBlockingQueue<>(WORKER_QUEUE_CAPACITY),
        new ThreadFactoryBuilder().setNameFormat("graph-engine-pool-%d").setDaemon(true).build(),
        new ThreadPoolExecutor.AbortPolicy());

    private static final ScheduledExecutorService timeoutThreadPool = Executors.newSingleThreadScheduledExecutor(
        new ThreadFactoryBuilder().setNameFormat("graph-timeout-pool-%d").setDaemon(true).build());

    private final GraphContext context = new GraphContext();
    private final Map<String, NodeStatus> nodeStatuses = new LinkedHashMap<>();
    private final GraphTraceContext traceContext;
    private final GraphTraceObserver traceObserver;
    private GraphExecutionTrace trace;
    private GraphPlan preparedPlan;

    private GraphEngine(GraphTraceContext traceContext, GraphTraceObserver traceObserver) {
        this.traceContext = traceContext;
        this.traceObserver = traceObserver == null ? GraphTraceObserver.NOOP : traceObserver;
    }

    public static GraphEngine getSessionGraphEngine() {
        return new GraphEngine(GraphTraceContext.create(null, null, null, null, null), GraphTraceObserver.NOOP);
    }

    public static GraphEngine getSessionGraphEngine(GraphTraceContext context, GraphTraceObserver observer) {
        return new GraphEngine(context, observer);
    }

    public void prepare(Object paramsObj) {
        if (paramsObj != null) {
            for (Field field : paramsObj.getClass().getDeclaredFields()) {
                try {
                    field.setAccessible(true);
                    context.addParam(field.getName(), field.get(paramsObj));
                } catch (IllegalAccessException error) {
                    throw new IllegalStateException(error);
                }
            }
        }
    }

    public void addParam(String key, Object value) {
        context.addParam(key, value);
    }

    /** Retained for callers using the original build-then-execute API. */
    public void buildGraph(GraphConfig graphConfig) {
        try {
            buildGraph(GraphPlan.compile(graphConfig));
        } catch (IllegalArgumentException error) {
            log.error("compile graph failed: {}", ExceptionUtils.getStackTrace(error));
            preparedPlan = null;
        }
    }

    public void buildGraph(GraphPlan plan) {
        preparedPlan = plan;
    }

    public void execGraph() {
        if (preparedPlan != null)
            execGraph(preparedPlan);
    }

    public void execGraph(GraphPlan plan) {
        execGraph(plan, Long.MAX_VALUE);
    }

    /** Executes a precompiled plan within a request-wide deadline. */
    public void execGraph(GraphPlan plan, long deadlineMillis) {
        long graphStartedNanos = System.nanoTime();
        long deadlineNanos = deadlineMillis == Long.MAX_VALUE ? Long.MAX_VALUE
            : System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(Math.max(0L, deadlineMillis));
        Node[] nodes = new Node[plan.size()];
        NodeExecution[] executions = new NodeExecution[plan.size()];
        for (int index = 0; index < nodes.length; index++) {
            NodeConfig config = plan.getNodeConfig(index);
            context.addConfig(config.getName(), config);
            nodes[index] = plan.newNode(index);
            executions[index] = new NodeExecution(nodes[index]);
        }

        try {
            int[] dependencies = plan.newDependencyCounts();
            boolean[] blocked = new boolean[plan.size()];
            List<Integer> ready = indexes(plan.getRoots());
            while (!ready.isEmpty()) {
                if (remainingMillis(deadlineNanos) <= 0L) {
                    cancelUnfinished(executions);
                    break;
                }

                CountDownLatch latch = new CountDownLatch(countRunnable(ready, blocked));
                for (int index : ready) {
                    NodeExecution execution = executions[index];
                    execution.queued();
                    if (blocked[index]) {
                        execution.skip();
                        continue;
                    }
                    execution.bind(latch);
                    long remaining = remainingMillis(deadlineNanos);
                    submit(execution, Math.min(nodes[index].getTimeout(), remaining),
                        remaining <= nodes[index].getTimeout());
                }
                await(latch, executions);

                List<Integer> next = new ArrayList<>();
                for (int index : ready) {
                    NodeExecution execution = executions[index];
                    nodeStatuses.put(execution.node.getName(), execution.status());
                    FailurePolicy policy = policy(execution.node.getConfig());
                    if (!execution.succeeded() && policy == FailurePolicy.FAIL_GRAPH) {
                        cancelUnfinished(executions);
                        throw new GraphExecutionException(execution.node.getName(), execution.status(),
                            execution.error());
                    }
                    boolean blocksChildren =
                        blocked[index] || !execution.succeeded() && policy == FailurePolicy.SKIP_DESCENDANTS;
                    for (int child : plan.getChildren(index)) {
                        blocked[child] |= blocksChildren;
                        if (--dependencies[child] == 0)
                            next.add(child);
                    }
                }
                ready = next;
            }
        } finally {
            for (NodeExecution execution : executions)
                nodeStatuses.put(execution.node.getName(), execution.status());
            trace = buildTrace(executions, graphStartedNanos);
            try {
                traceObserver.onComplete(trace);
            } catch (RuntimeException observerError) {
                log.warn("graph trace observer failed", observerError);
            }
        }
    }

    private void submit(NodeExecution execution, long timeoutMillis, boolean requestDeadline) {
        if (timeoutMillis <= 0L) {
            execution.timeout();
            return;
        }
        try {
            execution.setRequestDeadline(requestDeadline);
            Future<?> future = threadPool.submit(() -> {
                execution.started();
                long start = System.currentTimeMillis();
                try {
                    if (execution.node instanceof TypedNode) {
                        TypedNode typed = (TypedNode)execution.node;
                        NodeInput input = context.snapshot(typed.requiredInputs());
                        NodeOutput output = typed.execute(input);
                        if (output == null)
                            throw new IllegalStateException("typed node returned null output");
                        execution.succeed(() -> context.commit(output), input.size(), valueCount(output.values()));
                    } else {
                        GraphContext localContext = context.forkExecution();
                        int inputCount = localContext.importNodeData(execution.node);
                        execution.node.run(localContext);
                        Map<String, Object> output = localContext.extractNodeData(execution.node);
                        execution.succeed(() -> context.commitExecution(localContext, output), inputCount,
                            valueCount(output));
                    }
                } catch (Throwable error) {
                    log.error("node:{} exec with exception:{}", execution.node.getName(),
                        ExceptionUtils.getStackTrace(error));
                    execution.fail(error);
                } finally {
                    log.info("node:{} exec cost time: {}ms", execution.node.getName(),
                        System.currentTimeMillis() - start);
                }
            });
            execution.setFuture(future);
            timeoutThreadPool.schedule(execution::timeout, timeoutMillis, TimeUnit.MILLISECONDS);
        } catch (RejectedExecutionException error) {
            execution.fail(error);
        }
    }

    private static void await(CountDownLatch latch, NodeExecution[] executions) {
        try {
            latch.await();
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            cancelUnfinished(executions);
            throw new GraphExecutionException("graph", NodeStatus.CANCELLED, error);
        }
    }

    private static int countRunnable(List<Integer> ready, boolean[] blocked) {
        int count = 0;
        for (int index : ready)
            if (!blocked[index])
                count++;
        return count;
    }

    private static List<Integer> indexes(int[] values) {
        List<Integer> indexes = new ArrayList<>(values.length);
        for (int value : values)
            indexes.add(value);
        return indexes;
    }

    private static long remainingMillis(long deadlineNanos) {
        if (deadlineNanos == Long.MAX_VALUE)
            return Long.MAX_VALUE;
        long nanos = deadlineNanos - System.nanoTime();
        return nanos <= 0L ? 0L : Math.max(1L, TimeUnit.NANOSECONDS.toMillis(nanos));
    }

    private static FailurePolicy policy(NodeConfig config) {
        return config.getFailurePolicy() == null ? FailurePolicy.CONTINUE : config.getFailurePolicy();
    }

    private static void cancelUnfinished(NodeExecution[] executions) {
        for (NodeExecution execution : executions)
            execution.cancel();
    }

    public Map<String, NodeStatus> getNodeStatuses() {
        return new LinkedHashMap<>(nodeStatuses);
    }

    public Object getData(String key) {
        return context.getData(key);
    }

    public <T> T getData(com.openrec.graph.data.DataKey<T> key) {
        return context.getData(key);
    }

    public GraphExecutionTrace getTrace() {
        return trace;
    }

    private GraphExecutionTrace buildTrace(NodeExecution[] executions, long graphStartedNanos) {
        List<NodeExecutionTrace> nodes = new ArrayList<>(executions.length);
        boolean deadlineExceeded = false;
        for (NodeExecution execution : executions) {
            nodes.add(execution.trace());
            deadlineExceeded |= execution.deadlineExceeded();
        }
        return new GraphExecutionTrace(traceContext,
            TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - graphStartedNanos), deadlineExceeded, nodes);
    }

    private static int valueCount(Map<?, ?> values) {
        int count = 0;
        for (Object value : values.values()) {
            if (value instanceof java.util.Collection)
                count += ((java.util.Collection<?>)value).size();
            else if (value instanceof Map)
                count += ((Map<?, ?>)value).size();
            else if (value != null)
                count++;
        }
        return count;
    }

    public <T> T getResult() {
        return (T)context.getResult();
    }

    public void refresh() {
        // Session reuse remains intentionally disabled.
    }

    public void destroy() {
        preparedPlan = null;
        nodeStatuses.clear();
        context.clean();
    }

    private static final class NodeExecution {
        private final Node node;
        private NodeStatus status = NodeStatus.INIT;
        private Throwable error;
        private Future<?> future;
        private CountDownLatch latch;
        private long queuedNanos;
        private long startedNanos;
        private long finishedNanos;
        private int inputCount;
        private int outputCount;
        private boolean requestDeadline;

        private NodeExecution(Node node) {
            this.node = node;
        }

        private synchronized void queued() {
            queuedNanos = System.nanoTime();
        }

        private synchronized void started() {
            startedNanos = System.nanoTime();
        }

        private synchronized void bind(CountDownLatch value) {
            latch = value;
            status = NodeStatus.RUNNING;
            node.start();
        }

        private synchronized void setFuture(Future<?> value) {
            future = value;
            if (status == NodeStatus.TIMED_OUT || status == NodeStatus.CANCELLED)
                future.cancel(true);
        }

        private synchronized void setRequestDeadline(boolean value) {
            requestDeadline = value;
        }

        private synchronized void succeed(Runnable commit, int inputs, int outputs) {
            if (terminal())
                return;
            commit.run();
            inputCount = inputs;
            outputCount = outputs;
            complete(NodeStatus.SUCCESS, null);
        }

        private synchronized void fail(Throwable value) {
            if (!terminal())
                complete(NodeStatus.FAILED, value);
        }

        private synchronized void timeout() {
            if (terminal())
                return;
            complete(NodeStatus.TIMED_OUT, null);
            log.warn("graph node:{} timed out", node.getName());
            if (future != null)
                future.cancel(true);
        }

        private synchronized void cancel() {
            if (terminal())
                return;
            complete(NodeStatus.CANCELLED, null);
            log.warn("graph node:{} cancelled by request deadline", node.getName());
            if (future != null)
                future.cancel(true);
        }

        private synchronized void skip() {
            if (!terminal())
                complete(NodeStatus.SKIPPED, null);
        }

        private void complete(NodeStatus value, Throwable cause) {
            status = value;
            error = cause;
            finishedNanos = System.nanoTime();
            node.complete(value);
            if (latch != null)
                latch.countDown();
        }

        private boolean terminal() {
            return status.isTerminal();
        }

        private synchronized boolean succeeded() {
            return status == NodeStatus.SUCCESS;
        }

        private synchronized NodeStatus status() {
            return status;
        }

        private synchronized Throwable error() {
            return error;
        }

        private synchronized boolean deadlineExceeded() {
            return requestDeadline && (status == NodeStatus.TIMED_OUT || status == NodeStatus.CANCELLED);
        }

        private synchronized NodeExecutionTrace trace() {
            long effectiveStart = startedNanos == 0L ? finishedNanos : startedNanos;
            long effectiveQueued = queuedNanos == 0L ? effectiveStart : queuedNanos;
            long effectiveFinish = finishedNanos == 0L ? System.nanoTime() : finishedNanos;
            String type = node.getConfig().getType();
            if (type == null || type.trim().isEmpty())
                type = node.getConfig().getClazz();
            return new NodeExecutionTrace(node.getName(), type, status,
                TimeUnit.NANOSECONDS.toMillis(Math.max(0L, effectiveStart - effectiveQueued)),
                TimeUnit.NANOSECONDS.toMillis(Math.max(0L, effectiveFinish - effectiveStart)), inputCount, outputCount,
                error == null ? null : error.getClass().getSimpleName());
        }
    }
}
