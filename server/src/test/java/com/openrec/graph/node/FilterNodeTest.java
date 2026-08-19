package com.openrec.graph.node;

import java.util.Map;

import org.junit.Test;

import com.google.common.collect.Maps;
import com.openrec.graph.GraphContext;
import com.openrec.graph.RecEventType;
import com.openrec.graph.config.FilterConfig;
import com.openrec.graph.config.NodeConfig;

public class FilterNodeTest {

    @Test
    public void run() {
        FilterConfig filterConfig = new FilterConfig();
        Map<String, FilterConfig.TypeFilterConfig> filterMap = Maps.newHashMap();
        filterMap.put(RecEventType.EXPOSE.toString(), new FilterConfig.TypeFilterConfig());
        filterConfig.setFilterMap(filterMap);

        GraphContext context = new GraphContext();
        context.addParam("scene", "scene-1");
        context.addParam("userId", "userId-1");

        NodeConfig<FilterConfig> nodeConfig = new NodeConfig<>();
        nodeConfig.setContent(filterConfig);
        nodeConfig.setOpen(false);
        context.addConfig("filter", nodeConfig);

        FilterNode filterNode = new FilterNode(nodeConfig);
        filterNode.run(context);
    }
}
