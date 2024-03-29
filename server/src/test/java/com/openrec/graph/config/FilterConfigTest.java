package com.openrec.graph.config;

import org.junit.Assert;
import org.junit.Test;

import com.google.gson.Gson;
import com.openrec.graph.RecEventType;

public class FilterConfigTest {

    private String TEST_FILTER_CONFIG =
        "{\n" + "\t\"name\": \"a\",\n" + "\t\"clazz\": \"com.openrec.graph.node.EmptyNode\",\n" + "\t\"open\": true,\n"
            + "\t\"content\": {\n" + "\t\t\"filterMap\": {\n" + "\t\t\t\"expose\": {\n"
            + "\t\t\t\t\"duration\": 86400,\n" + "\t\t\t\t\"size\": 1000\n" + "\t\t\t}\n" + "\t\t}\n" + "\t}\n" + "}";

    @Test
    public void test() {
        NodeConfig<FilterConfig> nodeConfig =
            new Gson().fromJson(TEST_FILTER_CONFIG, NodeConfigTool.getNodeConfigType(FilterConfig.class));
        Assert.assertEquals(nodeConfig.getName(), "a");
        Assert.assertEquals(nodeConfig.getContent().getFilterMap().get(RecEventType.EXPOSE.toString()).getDuration(),
            86400);
    }
}