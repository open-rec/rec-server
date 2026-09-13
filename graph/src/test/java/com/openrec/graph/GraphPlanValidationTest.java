package com.openrec.graph;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.Collections;

import org.junit.Test;

import com.openrec.graph.config.NodeConfig;

public class GraphPlanValidationTest {

    @Test
    public void rejectsDuplicateNodesEdgesAndCycles() {
        GraphConfig duplicateNodes = graph(node("a"), node("a"));
        assertRejected(duplicateNodes, "duplicate node");

        GraphConfig duplicateEdges = graph(node("a"), node("b"));
        duplicateEdges.setEdges(Arrays.asList(edge("a", "b"), edge("a", "b")));
        assertRejected(duplicateEdges, "duplicate edge");

        GraphConfig cyclic = graph(node("a"), node("b"));
        cyclic.setEdges(Arrays.asList(edge("a", "b"), edge("b", "a")));
        assertRejected(cyclic, "cycle");
    }

    @Test
    public void rejectsInvalidTimeoutAndUnknownEndpoints() {
        NodeConfig<Object> invalid = node("invalid");
        invalid.setTimeout(0);
        assertRejected(graph(invalid), "timeout");

        GraphConfig unknown = graph(node("a"));
        unknown.setEdges(Collections.singletonList(edge("a", "missing")));
        assertRejected(unknown, "unknown node");
    }

    private static void assertRejected(GraphConfig config, String message) {
        try {
            GraphPlan.compile(config);
            fail("expected graph rejection");
        } catch (IllegalArgumentException error) {
            assertTrue(error.getMessage().contains(message));
        }
    }

    private static GraphConfig graph(NodeConfig<?>... nodes) {
        GraphConfig graph = new GraphConfig();
        graph.setNodes(Arrays.<NodeConfig>asList(nodes));
        graph.setEdges(Collections.emptyList());
        return graph;
    }

    private static NodeConfig<Object> node(String name) {
        NodeConfig<Object> node = new NodeConfig<>();
        node.setName(name);
        node.setClazz("com.openrec.graph.node.EmptyNode");
        node.setOpen(true);
        node.setTimeout(100);
        return node;
    }

    private static GraphConfig.NodeEdge edge(String from, String to) {
        GraphConfig.NodeEdge edge = new GraphConfig.NodeEdge();
        edge.setFrom(from);
        edge.setTo(to);
        return edge;
    }
}
