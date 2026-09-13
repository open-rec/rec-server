package com.openrec.graph;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

import com.openrec.graph.config.NodeConfig;
import com.openrec.graph.node.AbstractSyncNode;
import com.openrec.graph.node.NodeStatus;
import com.openrec.graph.tools.anno.Export;
import com.openrec.graph.tools.anno.Import;

public class AnnotatedNodeContractTest {

    @Test
    public void compilesAndExecutesCompatibleAnnotatedDataFlow() {
        GraphConfig graph = graph(node("source", StringSource.class), node("sink", StringSink.class));
        graph.setEdges(Collections.singletonList(edge("source", "sink")));

        GraphPlan plan = GraphPlan.compile(graph);
        GraphEngine engine = GraphEngine.getSessionGraphEngine();
        engine.execGraph(plan);

        assertEquals("value", engine.getResult());
        assertEquals(NodeStatus.SUCCESS, engine.getNodeStatuses().get("sink"));
    }

    @Test
    public void rejectsMissingOrUnreachableRequiredImport() {
        assertRejected(graph(node("sink", StringSink.class)), "no upstream node exports it");

        GraphConfig disconnected = graph(node("source", StringSource.class), node("sink", StringSink.class));
        assertRejected(disconnected, "no upstream node exports it");
    }

    @Test
    public void permitsMissingOptionalImport() {
        GraphPlan.compile(graph(node("optional", OptionalSink.class)));
    }

    @Test
    public void failsNodeWhenRequiredExportIsAbsentAtRuntime() {
        GraphConfig graph = graph(node("source", NullSource.class), node("sink", StringSink.class));
        graph.setEdges(Collections.singletonList(edge("source", "sink")));
        GraphEngine engine = GraphEngine.getSessionGraphEngine();

        engine.execGraph(GraphPlan.compile(graph));

        assertEquals(NodeStatus.FAILED, engine.getNodeStatuses().get("sink"));
    }

    @Test
    public void rejectsRawAndGenericTypeMismatch() {
        GraphConfig rawMismatch = graph(node("source", IntegerSource.class), node("sink", StringSink.class));
        rawMismatch.setEdges(Collections.singletonList(edge("source", "sink")));
        assertRejected(rawMismatch, "java.lang.String");

        GraphConfig genericMismatch =
            graph(node("source", StringListSource.class), node("sink", IntegerListSink.class));
        genericMismatch.setEdges(Collections.singletonList(edge("source", "sink")));
        assertRejected(genericMismatch, "java.util.List<java.lang.Integer>");
    }

    @Test
    public void discoversInheritedAnnotationsAndRejectsDuplicatePorts() {
        GraphConfig inherited = graph(node("source", InheritedSource.class), node("sink", StringSink.class));
        inherited.setEdges(Collections.singletonList(edge("source", "sink")));
        GraphPlan.compile(inherited);

        assertRejected(graph(node("duplicate", DuplicateImportNode.class)), "duplicate import");
    }

    private static void assertRejected(GraphConfig graph, String message) {
        try {
            GraphPlan.compile(graph);
            fail("expected graph rejection");
        } catch (IllegalArgumentException error) {
            assertTrue(error.getMessage(), error.getMessage().contains(message));
        }
    }

    private static GraphConfig graph(NodeConfig<?>... nodes) {
        GraphConfig graph = new GraphConfig();
        graph.setNodes(Arrays.<NodeConfig>asList(nodes));
        graph.setEdges(Collections.emptyList());
        return graph;
    }

    private static NodeConfig<Object> node(String name, Class<?> type) {
        NodeConfig<Object> node = new NodeConfig<>();
        node.setName(name);
        node.setClazz(type.getName());
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

    public static class StringSource extends AbstractSyncNode<Void> {
        @Export("value")
        private String value;

        public StringSource(NodeConfig<?> config) {
            super(config);
        }

        @Override
        public void run(GraphContext context) {
            value = "value";
        }
    }

    public static class IntegerSource extends AbstractSyncNode<Void> {
        @Export("value")
        private Integer value;

        public IntegerSource(NodeConfig<?> config) {
            super(config);
        }

        @Override
        public void run(GraphContext context) {
            value = 1;
        }
    }

    public static class NullSource extends AbstractSyncNode<Void> {
        @Export("value")
        private String value;

        public NullSource(NodeConfig<?> config) {
            super(config);
        }

        @Override
        public void run(GraphContext context) {}
    }

    public static class StringSink extends AbstractSyncNode<Void> {
        @Import("value")
        private String value;

        public StringSink(NodeConfig<?> config) {
            super(config);
        }

        @Override
        public void run(GraphContext context) {
            context.setResult(value);
        }
    }

    public static class OptionalSink extends AbstractSyncNode<Void> {
        @Import(value = "optional", required = false)
        private String value;

        public OptionalSink(NodeConfig<?> config) {
            super(config);
        }

        @Override
        public void run(GraphContext context) {}
    }

    public static class StringListSource extends AbstractSyncNode<Void> {
        @Export("values")
        private List<String> values;

        public StringListSource(NodeConfig<?> config) {
            super(config);
        }

        @Override
        public void run(GraphContext context) {
            values = Collections.singletonList("value");
        }
    }

    public static class IntegerListSink extends AbstractSyncNode<Void> {
        @Import("values")
        private List<Integer> values;

        public IntegerListSink(NodeConfig<?> config) {
            super(config);
        }

        @Override
        public void run(GraphContext context) {}
    }

    public abstract static class InheritedExport extends AbstractSyncNode<Void> {
        @Export("value")
        protected String value;

        protected InheritedExport(NodeConfig<?> config) {
            super(config);
        }
    }

    public static class InheritedSource extends InheritedExport {
        public InheritedSource(NodeConfig<?> config) {
            super(config);
        }

        @Override
        public void run(GraphContext context) {
            value = "value";
        }
    }

    public static class DuplicateImportNode extends AbstractSyncNode<Void> {
        @Import(value = "duplicate", required = false)
        private String first;
        @Import(value = "duplicate", required = false)
        private String second;

        public DuplicateImportNode(NodeConfig<?> config) {
            super(config);
        }

        @Override
        public void run(GraphContext context) {}
    }
}
