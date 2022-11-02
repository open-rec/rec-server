package com.openrec.graph.node;

import com.openrec.RecServer;
import com.openrec.graph.GraphContext;
import com.openrec.graph.config.HotConfig;
import com.openrec.graph.config.NewConfig;
import com.openrec.graph.config.NodeConfig;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import static org.junit.Assert.*;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = RecServer.class)
public class NewNodeTest {

    @Test
    public void run() {
        NewConfig newConfig = new NewConfig();

        GraphContext context = new GraphContext();
        context.addParam("sceneId", "sceneId-1");

        NodeConfig<NewConfig> nodeConfig = new NodeConfig<>();
        nodeConfig.setContent(newConfig);
        nodeConfig.setOpen(true);
        context.addConfig("new", nodeConfig);

        NewNode newNode = new NewNode(nodeConfig);
        newNode.run(context);
    }
}