package com.openrec.graph;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.Collections;

import org.junit.Test;

import com.openrec.graph.config.NodeConfig;
import com.openrec.graph.node.AbstractSyncNode;
import com.openrec.graph.node.FailurePolicy;
import com.openrec.graph.node.NodeStatus;
import com.openrec.graph.tools.anno.Export;

public class GraphExecutionSemanticsTest {

    @Test
    public void requestDeadlineCancelsWorkAndRejectsLateOutput() throws Exception {
        GraphEngine engine = GraphEngine.getSessionGraphEngine();
        engine.execGraph(GraphPlan.compile(graph(node("slow", SlowExportNode.class, FailurePolicy.CONTINUE))), 25L);

        assertEquals(NodeStatus.TIMED_OUT, engine.getNodeStatuses().get("slow"));
        Thread.sleep(150L);
        assertNull(engine.getData("late"));
    }

    @Test
    public void skipDescendantsDoesNotBlockIndependentBranch() {
        NodeConfig<Object> failure = node("failure", FailingNode.class, FailurePolicy.SKIP_DESCENDANTS);
        NodeConfig<Object> child = node("child", SuccessfulNode.class, FailurePolicy.CONTINUE);
        NodeConfig<Object> independent = node("independent", SuccessfulNode.class, FailurePolicy.CONTINUE);
        GraphConfig graph = graph(failure, child, independent);
        graph.setEdges(Collections.singletonList(edge("failure", "child")));

        GraphEngine engine = GraphEngine.getSessionGraphEngine();
        engine.execGraph(GraphPlan.compile(graph), 1000L);

        assertEquals(NodeStatus.FAILED, engine.getNodeStatuses().get("failure"));
        assertEquals(NodeStatus.SKIPPED, engine.getNodeStatuses().get("child"));
        assertEquals(NodeStatus.SUCCESS, engine.getNodeStatuses().get("independent"));
    }

    @Test
    public void failGraphPolicyTerminatesRequest() {
        GraphEngine engine = GraphEngine.getSessionGraphEngine();
        try {
            engine.execGraph(GraphPlan.compile(graph(node("failure", FailingNode.class, FailurePolicy.FAIL_GRAPH))),
                1000L);
            fail("expected graph failure");
        } catch (GraphExecutionException error) {
            assertEquals("failure", error.getNode());
            assertEquals(NodeStatus.FAILED, error.getStatus());
        }
    }

    private static GraphConfig graph(NodeConfig<?>... nodes) {
        GraphConfig graph = new GraphConfig();
        graph.setNodes(Arrays.<NodeConfig>asList(nodes));
        graph.setEdges(Collections.emptyList());
        return graph;
    }

    private static NodeConfig<Object> node(String name, Class<?> type, FailurePolicy policy) {
        NodeConfig<Object> node = new NodeConfig<>();
        node.setName(name);
        node.setClazz(type.getName());
        node.setOpen(true);
        node.setTimeout(500);
        node.setFailurePolicy(policy);
        return node;
    }

    private static GraphConfig.NodeEdge edge(String from, String to) {
        GraphConfig.NodeEdge edge = new GraphConfig.NodeEdge();
        edge.setFrom(from);
        edge.setTo(to);
        return edge;
    }

    public static class SlowExportNode extends AbstractSyncNode<Object> {
        @Export("late")
        private String value;

        public SlowExportNode(NodeConfig config) {
            super(config);
        }

        @Override
        public void run(GraphContext context) {
            long until = System.nanoTime() + java.util.concurrent.TimeUnit.MILLISECONDS.toNanos(100L);
            while (System.nanoTime() < until) {
                // Deliberately ignore interruption to model a non-cooperative client.
            }
            value = "should-not-commit";
        }
    }

    public static class FailingNode extends AbstractSyncNode<Object> {
        public FailingNode(NodeConfig config) {
            super(config);
        }

        @Override
        public void run(GraphContext context) {
            throw new IllegalStateException("boom");
        }
    }

    public static class SuccessfulNode extends AbstractSyncNode<Object> {
        public SuccessfulNode(NodeConfig config) {
            super(config);
        }

        @Override
        public void run(GraphContext context) {}
    }
}
