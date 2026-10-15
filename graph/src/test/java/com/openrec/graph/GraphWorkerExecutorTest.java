package com.openrec.graph;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import com.openrec.graph.config.NodeConfig;
import com.openrec.graph.node.AbstractSyncNode;
import com.openrec.graph.node.NodeStatus;

public class GraphWorkerExecutorTest {

    @Test
    public void defaultsToNamedVirtualThreads() throws Exception {
        try (var executor = GraphWorkerExecutor.create(key -> null, key -> null)) {
            assertTrue(executor.submit(() -> Thread.currentThread().isVirtual()).get());
            assertTrue(
                executor.submit(() -> Thread.currentThread().getName()).get().startsWith("graph-engine-virtual-"));
        }
    }

    @Test
    public void environmentSelectsPlatformAndPropertiesOverrideIt() throws Exception {
        Map<String, String> environment = Map.of("OPENREC_GRAPH_EXECUTOR", "platform", "OPENREC_GRAPH_WORKER_THREADS",
            "1", "OPENREC_GRAPH_WORKER_QUEUE_CAPACITY", "1");
        try (var executor = GraphWorkerExecutor.create(key -> null, environment::get)) {
            assertFalse(executor.submit(() -> Thread.currentThread().isVirtual()).get());
            assertTrue(executor.submit(() -> Thread.currentThread().isDaemon()).get());
        }
        try (var executor =
            GraphWorkerExecutor.create(Map.of("openrec.graph.executor", "virtual")::get, environment::get)) {
            assertTrue(executor.submit(() -> Thread.currentThread().isVirtual()).get());
        }
    }

    @Test
    public void platformPreservesBoundedQueueAndRejection() throws Exception {
        Map<String, String> properties = Map.of("openrec.graph.executor", "platform", "openrec.graph.worker.threads",
            "1", "openrec.graph.worker.queue-capacity", "1");
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        var executor = GraphWorkerExecutor.create(properties::get, key -> null);
        try {
            var first = executor.submit(() -> {
                started.countDown();
                release.await();
                return null;
            });
            assertTrue(started.await(5, TimeUnit.SECONDS));
            var queued = executor.submit(() -> {
            });
            assertThrows(RejectedExecutionException.class, () -> executor.submit(() -> {
            }));
            release.countDown();
            first.get(5, TimeUnit.SECONDS);
            queued.get(5, TimeUnit.SECONDS);
        } finally {
            release.countDown();
            executor.shutdownNow();
        }
    }

    @Test
    public void invalidConfigurationFailsExplicitly() {
        assertThrows(IllegalArgumentException.class,
            () -> GraphWorkerExecutor.create(Map.of("openrec.graph.executor", "typo")::get, key -> null));
        for (String key : new String[] {"openrec.graph.worker.threads", "openrec.graph.worker.queue-capacity"}) {
            for (String value : new String[] {"0", "-1", "invalid"}) {
                Map<String, String> properties = Map.of("openrec.graph.executor", "platform", key, value);
                assertThrows(IllegalArgumentException.class,
                    () -> GraphWorkerExecutor.create(properties::get, ignored -> null));
            }
        }
    }

    @Test
    public void graphUsesConfiguredWorkerMode() {
        NodeConfig<Object> node = new NodeConfig<>();
        node.setName("thread");
        node.setClazz(ThreadNode.class.getName());
        node.setOpen(true);
        node.setTimeout(2000);
        GraphConfig config = new GraphConfig();
        config.setNodes(Collections.singletonList(node));
        config.setEdges(Collections.emptyList());
        GraphEngine engine = GraphEngine.getSessionGraphEngine();
        engine.execGraph(GraphPlan.compile(config), 2000);
        assertEquals(NodeStatus.SUCCESS, engine.getNodeStatuses().get("thread"));
        String mode = System.getProperty("openrec.graph.executor",
            System.getenv().getOrDefault("OPENREC_GRAPH_EXECUTOR", "virtual"));
        assertEquals("virtual".equalsIgnoreCase(mode.trim()), engine.getData("virtual"));
    }

    public static class ThreadNode extends AbstractSyncNode<Object> {
        public ThreadNode(NodeConfig config) {
            super(config);
        }

        @Override
        public void run(GraphContext context) {
            context.addData("virtual", Thread.currentThread().isVirtual());
        }
    }
}
