package com.openrec.graph.node;

import com.openrec.RecServer;
import com.openrec.graph.GraphContext;
import com.openrec.graph.config.HotConfig;
import com.openrec.graph.config.NodeConfig;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = RecServer.class)
public class HotNodeTest {

    @Test
    public void run() {
        HotConfig hotConfig = new HotConfig();

        GraphContext context = new GraphContext();
        context.addParam("scene", "scene-1");

        NodeConfig<HotConfig> nodeConfig = new NodeConfig<>();
        nodeConfig.setContent(hotConfig);
        nodeConfig.setOpen(true);
        context.addConfig("hot", nodeConfig);

        HotNode hotNode = new HotNode(nodeConfig);
        hotNode.run(context);
    }
}