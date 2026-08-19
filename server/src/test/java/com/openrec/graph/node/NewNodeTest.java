package com.openrec.graph.node;

import org.junit.Test;

import com.openrec.graph.GraphContext;
import com.openrec.graph.config.NewConfig;
import com.openrec.graph.config.NodeConfig;

public class NewNodeTest {

    @Test
    public void run() {
        NewConfig newConfig = new NewConfig();

        GraphContext context = new GraphContext();
        context.addParam("scene", "scene-1");

        NodeConfig<NewConfig> nodeConfig = new NodeConfig<>();
        nodeConfig.setContent(newConfig);
        nodeConfig.setOpen(false);
        context.addConfig("new", nodeConfig);

        NewNode newNode = new NewNode(nodeConfig);
        newNode.run(context);
    }
}
