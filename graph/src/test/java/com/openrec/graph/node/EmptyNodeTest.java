package com.openrec.graph.node;

import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.*;

public class EmptyNodeTest {

    @Test
    public void test() {
        EmptyNode node = new EmptyNode();
        Assert.assertEquals(NodeStatus.INIT, node.getStatus());

        Assert.assertTrue(node.isReady());
        if(node.isReady()){
            node.start();
            Assert.assertEquals(NodeStatus.RUNNING, node.getStatus());
            node.run(null);
            node.destroy();
            Assert.assertTrue(node.finished());
        }
    }
}