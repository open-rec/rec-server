package com.openrec.graph;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;

import com.openrec.graph.config.NodeConfig;
import com.openrec.graph.node.AbstractSyncNode;
import com.openrec.graph.node.NodeStatus;
import com.openrec.graph.trace.GraphExecutionTrace;
import com.openrec.graph.trace.GraphTraceContext;

public class GraphTraceTest {
    @Test
    public void reportsRequestMetadataAndNodeDetails() {
        AtomicReference<GraphExecutionTrace> observed = new AtomicReference<>();
        GraphTraceContext context = GraphTraceContext.create("request-1", "item", "home", "experiment-a", "v2");
        GraphEngine engine = GraphEngine.getSessionGraphEngine(context, observed::set);
        engine.execGraph(GraphPlan.compile(graph()), 1000L);

        GraphExecutionTrace trace = observed.get();
        assertEquals(trace, engine.getTrace());
        assertEquals("request-1", trace.getContext().getRequestId());
        assertEquals("experiment-a", trace.getContext().getExperiment());
        assertEquals(1, trace.getNodes().size());
        assertEquals("trace-node", trace.getNodes().get(0).getNodeName());
        assertEquals(NodeStatus.SUCCESS, trace.getNodes().get(0).getStatus());
        assertTrue(trace.getNodes().get(0).getExecutionMillis() >= 0L);
    }

    private static GraphConfig graph() {
        NodeConfig<Object> node = new NodeConfig<>();
        node.setName("trace-node");
        node.setClazz(TraceNode.class.getName());
        node.setOpen(true);
        node.setTimeout(100);
        GraphConfig graph = new GraphConfig();
        graph.setNodes(Collections.<NodeConfig>singletonList(node));
        graph.setEdges(Collections.emptyList());
        return graph;
    }

    public static class TraceNode extends AbstractSyncNode<Object> {
        public TraceNode(NodeConfig config) {
            super(config);
        }

        @Override
        public void run(GraphContext context) {}
    }
}
