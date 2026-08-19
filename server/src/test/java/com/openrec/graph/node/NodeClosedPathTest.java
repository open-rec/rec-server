package com.openrec.graph.node;

import com.openrec.graph.GraphContext;
import com.openrec.graph.config.NodeConfig;
import org.junit.Test;

import java.lang.reflect.Constructor;
import java.util.Arrays;

import static org.junit.Assert.assertNotNull;

public class NodeClosedPathTest {
    @Test
    public void everyRecommendationNodeCanBeDisabledWithoutDependencies() throws Exception {
        for (Class<? extends com.openrec.graph.node.Node> type : Arrays.asList(
            BlackNode.class, EmbeddingNode.class, FilterNode.class, HotNode.class,
            NewNode.class, UserTriggerNode.class)) {
            NodeConfig<Object> config = new NodeConfig<>();
            config.setName(type.getSimpleName());
            config.setOpen(false);
            Constructor<? extends com.openrec.graph.node.Node> constructor = type.getConstructor(NodeConfig.class);
            com.openrec.graph.node.Node node = constructor.newInstance(config);
            node.run(new GraphContext());
            assertNotNull(node.getName());
        }
    }
}
