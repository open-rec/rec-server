package com.openrec.graph;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.openrec.graph.config.NodeConfig;
import com.openrec.graph.data.DataKey;
import com.openrec.graph.data.NodeInput;
import com.openrec.graph.data.NodeOutput;
import com.openrec.graph.node.AbstractSyncNode;
import com.openrec.graph.node.NodeRegistry;
import com.openrec.graph.node.SimpleNodeFactory;
import com.openrec.graph.node.TypedNode;

public class NodeRegistryAndTypedIoTest {
    private static final DataKey<String> VALUE = DataKey.of("value", String.class);

    @Test
    public void executesRegisteredTypedNodesWithImmutableCommittedOutput() {
        NodeRegistry registry = NodeRegistry.builder().register(new SimpleNodeFactory("source", SourceNode::new))
            .register(new SimpleNodeFactory("sink", SinkNode::new)).build();
        GraphConfig graph = graph(node("source", "source"), node("sink", "sink"));
        GraphEngine engine = GraphEngine.getSessionGraphEngine();
        engine.execGraph(GraphPlan.compile(graph, registry));
        Assert.assertEquals("immutable", engine.getResult());
        Assert.assertEquals("immutable", engine.getData(VALUE));
    }

    @Test
    public void rejectsUnknownTypeAndMissingTypedInput() {
        NodeRegistry onlySink = NodeRegistry.builder().register(new SimpleNodeFactory("sink", SinkNode::new)).build();
        try {
            GraphPlan.compile(graph(node("missing", "unknown")), onlySink);
            Assert.fail("unknown type must fail");
        } catch (IllegalArgumentException expected) {
            Assert.assertTrue(expected.getMessage().contains("unknown node type"));
        }
        try {
            GraphPlan.compile(graph(node("sink", "sink")), onlySink);
            Assert.fail("missing input must fail");
        } catch (IllegalArgumentException expected) {
            Assert.assertTrue(expected.getMessage().contains("missing inputs"));
        }
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void outputDefensivelyCopiesCollections() {
        DataKey<List> items = DataKey.of("items", List.class);
        List<String> source = new java.util.ArrayList<>(Collections.singletonList("one"));
        NodeOutput output = NodeOutput.builder().put(items, source).build();
        source.add("two");
        List<String> committed = (List<String>)output.values().get(items);
        Assert.assertEquals(Collections.singletonList("one"), committed);
        try {
            committed.add("three");
            Assert.fail("committed collection must be immutable");
        } catch (UnsupportedOperationException expected) {
            // Expected.
        }
    }

    private static NodeConfig node(String name, String type) {
        NodeConfig config = new NodeConfig();
        config.setName(name);
        config.setType(type);
        config.setOpen(true);
        config.setTimeout(100);
        return config;
    }

    private static GraphConfig graph(NodeConfig... nodes) {
        GraphConfig graph = new GraphConfig();
        graph.setNodes(Arrays.asList(nodes));
        if (nodes.length == 2) {
            GraphConfig.NodeEdge edge = new GraphConfig.NodeEdge();
            edge.setFrom(nodes[0].getName());
            edge.setTo(nodes[1].getName());
            graph.setEdges(Collections.singletonList(edge));
        } else {
            graph.setEdges(Collections.emptyList());
        }
        return graph;
    }

    public static final class SourceNode extends AbstractSyncNode<Void> implements TypedNode {
        public SourceNode(NodeConfig config) {
            super(config);
        }

        @Override
        public Set<DataKey<?>> outputs() {
            return Collections.singleton(VALUE);
        }

        @Override
        public NodeOutput execute(NodeInput input) {
            return NodeOutput.builder().put(VALUE, "immutable").build();
        }

        @Override
        public void run(GraphContext context) {}
    }

    public static final class SinkNode extends AbstractSyncNode<Void> implements TypedNode {
        public SinkNode(NodeConfig config) {
            super(config);
        }

        @Override
        public Set<DataKey<?>> requiredInputs() {
            return Collections.singleton(VALUE);
        }

        @Override
        public NodeOutput execute(NodeInput input) {
            return NodeOutput.builder().result(input.require(VALUE)).build();
        }

        @Override
        public void run(GraphContext context) {}
    }
}
