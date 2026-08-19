package com.openrec.graph.node;

import com.openrec.graph.GraphContext;
import com.openrec.graph.config.NodeConfig;
import org.junit.Test;

import java.util.Collections;
import java.util.Map;

import static org.junit.Assert.*;

public class NodeLifecycleTest {
    @Test
    public void lifecycleRelationshipsAndRefreshAreConsistent() {
        NodeConfig<String> config = new NodeConfig<>();
        config.setName("configured");
        config.setOpen(true);
        config.setTimeout(25);
        EmptyNode node = new EmptyNode(config);
        EmptyNode parent = new EmptyNode();
        EmptyNode child = new EmptyNode();

        assertEquals("EmptyNode", node.getId());
        assertEquals("configured", node.getName());
        assertEquals(config, node.getConfig());
        assertEquals(25, node.getTimeout());
        assertFalse(node.finished());
        assertFalse(node.isRunning());
        node.addParent(parent);
        node.addChild(child);
        assertFalse(node.isReady());
        parent.stop();
        assertTrue(node.isReady());
        node.start();
        assertTrue(node.isRunning());
        node.run(new GraphContext());
        node.stop();
        assertTrue(node.finished());
        assertNotNull(node.toString());
        node.destroy();
        assertTrue(node.getParents().isEmpty());
        assertTrue(node.getChildren().isEmpty());

        node.refresh(null);
        assertEquals(NodeStatus.INIT, node.getStatus());
        assertEquals(5, node.getTimeout());
        node.setConfig(config);
        assertSame(config, node.getConfig());
    }

    @Test
    public void rootAndAsyncNodeImplementTheirContracts() {
        RootNode root = new RootNode();
        root.run(new GraphContext());
        AsyncNode<String> async = new AsyncNode<String>() {
            @Override public Map<String, String> buildQuery(GraphContext context) { return Collections.singletonMap("q", "v"); }
            @Override public void handleResult(GraphContext context, String result) { context.setResult(result); }
            @Override public void run(GraphContext context) { handleResult(context, buildQuery(context).get("q")); }
        };
        GraphContext context = new GraphContext();
        assertEquals("v", async.buildQuery(context).get("q"));
        async.run(context);
        assertEquals("v", context.getResult());
    }
}
