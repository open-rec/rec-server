package com.openrec.graph;

import com.google.gson.Gson;
import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.*;

public class GraphConfigTest {

    private static final String TEST_GRAPH_CONFIG = "{\n" +
            "\t\"nodes\": [{\n" +
            "\t\t\t\"name\": \"a\",\n" +
            "\t\t\t\"clazz\": \"com.openrec.graph.node.EmptyNode\",\n" +
            "\t\t\t\"open\": true,\n" +
            "\t\t\t\"content\": null\n" +
            "\t\t},\n" +
            "\t\t{\n" +
            "\t\t\t\"name\": \"b\",\n" +
            "\t\t\t\"clazz\": \"com.openrec.graph.node.EmptyNode\",\n" +
            "\t\t\t\"open\": true,\n" +
            "\t\t\t\"content\": null\n" +
            "\t\t},\n" +
            "\t\t{\n" +
            "\t\t\t\"name\": \"c\",\n" +
            "\t\t\t\"clazz\": \"com.openrec.graph.node.EmptyNode\",\n" +
            "\t\t\t\"open\": true,\n" +
            "\t\t\t\"content\": null\n" +
            "\t\t}\n" +
            "\t],\n" +
            "\t\"edges\": [{\n" +
            "\t\t\"from\": \"a\",\n" +
            "\t\t\"to\": \"c\"\n" +
            "\t}, {\n" +
            "\t\t\"from\": \"b\",\n" +
            "\t\t\"to\": \"c\"\n" +
            "\t}]\n" +
            "}";

    @Test
    public void build() {
        GraphConfig graphConfig = new Gson().fromJson(TEST_GRAPH_CONFIG, GraphConfig.class);
        Assert.assertEquals(graphConfig.getNodes().size(), 3);
        Assert.assertEquals(graphConfig.getEdges().size(), 2);
    }
}