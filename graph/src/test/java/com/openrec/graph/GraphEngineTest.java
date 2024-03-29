package com.openrec.graph;

import org.junit.Assert;
import org.junit.Test;

import com.google.gson.Gson;

public class GraphEngineTest {

    private static final String TEST_GRAPH_CONIG = "{\n" + "\t\"nodes\": [{\n" + "\t\t\t\"name\": \"a\",\n"
        + "\t\t\t\"clazz\": \"com.openrec.graph.node.SleepNode\",\n" + "\t\t\t\"open\": true,\n"
        + "\t\t\t\"content\": null\n" + "\t\t},\n" + "\t\t{\n" + "\t\t\t\"name\": \"b\",\n"
        + "\t\t\t\"clazz\": \"com.openrec.graph.node.SleepNode\",\n" + "\t\t\t\"open\": true,\n"
        + "\t\t\t\"content\": null\n" + "\t\t},\n" + "\t\t{\n" + "\t\t\t\"name\": \"c\",\n"
        + "\t\t\t\"clazz\": \"com.openrec.graph.node.SleepNode\",\n" + "\t\t\t\"open\": true,\n"
        + "\t\t\t\"content\": null\n" + "\t\t}\n" + "\t],\n" + "\t\"edges\": [{\n" + "\t\t\"from\": \"a\",\n"
        + "\t\t\"to\": \"c\"\n" + "\t}, {\n" + "\t\t\"from\": \"b\",\n" + "\t\t\"to\": \"c\"\n" + "\t}]\n" + "}";

    @Test
    public void testGraph() {
        GraphConfig graphConfig = new Gson().fromJson(TEST_GRAPH_CONIG, GraphConfig.class);
        long start = System.currentTimeMillis();
        GraphEngine graphEngine = GraphEngine.getSessionGraphEngine();
        graphEngine.prepare(null);
        graphEngine.buildGraph(graphConfig);
        graphEngine.execGraph();
        long cost = System.currentTimeMillis() - start;
        System.out.println(cost);
        Assert.assertTrue(cost < graphConfig.getNodes().size() * 1000);
    }
}