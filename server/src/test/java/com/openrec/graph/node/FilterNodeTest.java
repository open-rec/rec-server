package com.openrec.graph.node;

import com.google.common.collect.Maps;
import com.openrec.RecServer;
import com.openrec.graph.GraphContext;
import com.openrec.graph.RecEventType;
import com.openrec.graph.config.FilterConfig;
import com.openrec.graph.config.NodeConfig;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Map;

import static org.junit.Assert.*;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = RecServer.class)
public class FilterNodeTest {

    @Test
    public void run() {
        FilterConfig filterConfig = new FilterConfig();
        Map<String, FilterConfig.TypeFilterConfig> filterMap = Maps.newHashMap();
        filterMap.put(RecEventType.EXPOSE.toString(), new FilterConfig.TypeFilterConfig());
        filterConfig.setFilterMap(filterMap);

        GraphContext context = new GraphContext();
        context.addParam("sceneId", "sceneId-1");
        context.addParam("userId", "userId-1");

        NodeConfig<FilterConfig> nodeConfig = new NodeConfig<>();
        nodeConfig.setContent(filterConfig);
        nodeConfig.setOpen(true);
        context.addConfig("filter", nodeConfig);

        FilterNode filterNode = new FilterNode(nodeConfig);
        filterNode.run(context);
    }
}