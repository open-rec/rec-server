package com.openrec.service.rec;

import java.util.Collections;

import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.openrec.graph.GraphConfig;
import com.openrec.graph.GraphContext;
import com.openrec.graph.GraphPlan;
import com.openrec.graph.config.NodeConfig;
import com.openrec.graph.node.AbstractSyncNode;
import com.openrec.proto.biz.recommend.RecommendReq;

import static org.junit.Assert.*;

public class RecServiceResultTest {
    @Test
    public void expiredDeadlineMustNotReturnSuccessfulNullResult() {
        RecService service = new RecService();
        ReflectionTestUtils.setField(service, "recommendDeadlineMillis", 0L);
        IllegalStateException error =
            assertThrows(IllegalStateException.class, () -> service.execute(new RecommendReq(), plan()));
        assertTrue(error.getMessage().contains("collector=CANCELLED"));
    }

    @Test
    public void completedEmptyResultRemainsValid() {
        assertEquals(Collections.emptyList(), new RecService().execute(new RecommendReq(), plan()).getResults());
    }

    private static GraphPlan plan() {
        NodeConfig node = new NodeConfig();
        node.setName("collector");
        node.setClazz(EmptyCollector.class.getName());
        node.setOpen(true);
        node.setTimeout(1000);
        GraphConfig graph = new GraphConfig();
        graph.setNodes(Collections.singletonList(node));
        graph.setEdges(Collections.emptyList());
        return GraphPlan.compile(graph);
    }

    public static class EmptyCollector extends AbstractSyncNode<Object> {
        public EmptyCollector(NodeConfig config) {
            super(config);
        }

        @Override
        public void run(GraphContext context) {
            context.setResult(Collections.emptyList());
        }
    }
}
