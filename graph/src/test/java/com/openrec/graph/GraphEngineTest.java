package com.openrec.graph;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;

import org.junit.Assert;
import org.junit.Test;

import com.google.gson.Gson;
import com.openrec.graph.config.NodeConfig;
import com.openrec.graph.node.Node;
import com.openrec.graph.node.NodeStatus;
import com.openrec.graph.node.SleepNode;

public class GraphEngineTest {

    private static final String TEST_GRAPH_CONIG = "{\n" + "\t\"nodes\": [{\n" + "\t\t\t\"name\": \"a\",\n"
        + "\t\t\t\"clazz\": \"com.openrec.graph.node.SleepNode\",\n" + "\t\t\t\"open\": true,\n"
        + "\t\t\t\"timeout\": 2000,\n"
        + "\t\t\t\"content\": null\n" + "\t\t},\n" + "\t\t{\n" + "\t\t\t\"name\": \"b\",\n"
        + "\t\t\t\"clazz\": \"com.openrec.graph.node.SleepNode\",\n" + "\t\t\t\"open\": true,\n"
        + "\t\t\t\"timeout\": 2000,\n"
        + "\t\t\t\"content\": null\n" + "\t\t},\n" + "\t\t{\n" + "\t\t\t\"name\": \"c\",\n"
        + "\t\t\t\"clazz\": \"com.openrec.graph.node.SleepNode\",\n" + "\t\t\t\"open\": true,\n"
        + "\t\t\t\"timeout\": 2000,\n"
        + "\t\t\t\"content\": null\n" + "\t\t}\n" + "\t],\n" + "\t\"edges\": [{\n" + "\t\t\"from\": \"a\",\n"
        + "\t\t\"to\": \"c\"\n" + "\t}, {\n" + "\t\t\"from\": \"b\",\n" + "\t\t\"to\": \"c\"\n" + "\t}]\n" + "}";

    @Test(timeout = 10000)
    public void testGraph() {
        GraphConfig graphConfig = new Gson().fromJson(TEST_GRAPH_CONIG, GraphConfig.class);
        GraphPlan plan = GraphPlan.compile(graphConfig);
        long start = System.currentTimeMillis();
        GraphEngine graphEngine = GraphEngine.getSessionGraphEngine();
        graphEngine.prepare(null);
        graphEngine.buildGraph(plan);
        graphEngine.execGraph();
        long cost = System.currentTimeMillis() - start;
        System.out.println(cost);
        Assert.assertTrue(cost < graphConfig.getNodes().size() * 1000);

        GraphEngine secondExecution = GraphEngine.getSessionGraphEngine();
        secondExecution.execGraph(plan);
        Assert.assertSame(graphConfig, plan.getConfig());
    }

    @Test
    public void timeoutBeforeWorkerStartsReleasesGraphWaiter() throws Exception {
        NodeConfig<Object> config = new NodeConfig<>();
        config.setName("not-started");
        config.setTimeout(0);
        Node node = new SleepNode(config);
        node.start();
        FutureTask<Void> future = new FutureTask<>(() -> null);
        CountDownLatch latch = new CountDownLatch(1);

        GraphEngine engine = GraphEngine.getSessionGraphEngine();
        engine.new TimeoutTask(node, future, latch).call();

        Assert.assertTrue(future.isCancelled());
        Assert.assertEquals(NodeStatus.STOP, node.getStatus());
        Assert.assertEquals(0, latch.getCount());
    }
}
