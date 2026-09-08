package com.openrec.graph;

import java.util.Set;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Test;

import com.openrec.graph.config.CombineConfig;
import com.openrec.graph.config.NodeConfig;

public class RecTemplateTest {

    @Test
    public void test() {
        GraphConfig graphConfig = RecTemplate.toGraphConfig();
        Assert.assertNotNull(graphConfig);
        GraphConfig graphConfig2 = RecTemplate.toGraphConfig();
        Assert.assertNotSame(graphConfig, graphConfig2);
        Assert.assertEquals(graphConfig.getNodes().size(), graphConfig2.getNodes().size());
        Assert.assertEquals(graphConfig.getEdges().size(), graphConfig2.getEdges().size());
    }

    @Test
    public void combineFeedsRankWhileItemFeatureRemainsIndependent() {
        GraphConfig graphConfig = RecTemplate.toGraphConfig();
        Set<String> edges = graphConfig.getEdges().stream().map(edge -> edge.getFrom() + "->" + edge.getTo())
            .collect(Collectors.toSet());

        Assert.assertTrue(edges.contains("combine->rank"));
        Assert.assertTrue(edges.contains("itemFeature->rank"));
        Assert.assertFalse(edges.contains("combine->itemFeature"));
        Assert.assertFalse(graphConfig.getEdges().stream().anyMatch(edge -> "itemFeature".equals(edge.getTo())));

        NodeConfig combineNode =
            graphConfig.getNodes().stream().filter(node -> "combine".equals(node.getName())).findFirst().orElse(null);
        Assert.assertNotNull(combineNode);
        Assert.assertTrue(combineNode.getContent() instanceof CombineConfig);
    }

    @Test
    public void userGraphReferencesLoadableNodeClassesAndValidEdges() {
        GraphConfig graphConfig = RecTemplate.toGraphConfig("user_graph.json");
        Assert.assertEquals(13, graphConfig.getNodes().size());
        Assert.assertNotNull(GraphPlan.compile(graphConfig));
        Set<String> classes = graphConfig.getNodes().stream().map(NodeConfig::getClazz).collect(Collectors.toSet());
        Assert.assertTrue(classes.contains("com.openrec.graph.node.user.BlackNode"));
        Assert.assertTrue(classes.contains("com.openrec.graph.node.user.EmbeddingNode"));
        Assert.assertTrue(classes.contains("com.openrec.graph.node.user.OperationNode"));
        Assert.assertFalse(classes.stream().anyMatch(name -> name.contains(".user.User")));
    }
}
