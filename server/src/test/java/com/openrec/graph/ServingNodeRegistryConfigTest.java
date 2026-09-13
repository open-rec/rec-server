package com.openrec.graph;

import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.Collections;

import org.junit.Test;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;

import com.openrec.graph.config.NodeConfig;
import com.openrec.graph.node.NodeRegistry;

public class ServingNodeRegistryConfigTest {

    @Test
    public void compilesBothPackagedGraphsWithoutReflectiveFallback() {
        AutowireCapableBeanFactory beans = mock(AutowireCapableBeanFactory.class);
        NodeRegistry registry = new ServingNodeRegistryConfig().servingNodeRegistry(beans);

        assertNotNull(GraphPlan.compile(RecTemplate.toGraphConfig("item_graph.json"), registry));
        assertNotNull(GraphPlan.compile(RecTemplate.toGraphConfig("user_graph.json"), registry));
        verify(beans, atLeastOnce()).autowireBean(any());
    }

    @Test
    public void acceptsStableTypeWithoutJavaClassName() {
        NodeRegistry registry =
            new ServingNodeRegistryConfig().servingNodeRegistry(mock(AutowireCapableBeanFactory.class));
        NodeConfig<Object> node = new NodeConfig<>();
        node.setName("feature");
        node.setType("item.user-feature");
        node.setOpen(true);
        node.setTimeout(20);
        GraphConfig graph = new GraphConfig();
        graph.setNodes(Collections.<NodeConfig>singletonList(node));
        graph.setEdges(Collections.emptyList());

        assertNotNull(GraphPlan.compile(graph, registry));
    }
}
