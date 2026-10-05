package com.openrec.service.rec;

import java.util.List;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;
import com.openrec.graph.GraphConfig;
import com.openrec.graph.GraphContext;
import com.openrec.graph.GraphPlan;
import com.openrec.graph.config.NodeConfig;
import com.openrec.graph.config.RecallConfig;
import com.openrec.graph.node.AbstractRecallNode;
import com.openrec.graph.node.AbstractSyncNode;
import com.openrec.proto.biz.recommend.RecommendReq;
import com.openrec.proto.biz.recommend.RecommendRes;
import com.openrec.proto.biz.recommend.RecallDiagnostic;
import com.openrec.proto.model.ScoreResult;
import com.openrec.service.redis.RedisService;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class RecallDiagnosticsTest {
    @Test
    public void overlappingRecallSurvivesFinalResultTruncationInDiagnostics() {
        RecommendRes<?> response = execute(true, "normal", true);
        assertEquals("selected-other-item", response.getResults().get(0).getId());
        assertEquals(2, response.getRecallDiagnostics().size());
        for (RecallDiagnostic diagnostic : response.getRecallDiagnostics()) {
            assertEquals("SUCCESS", diagnostic.getStatus());
            assertEquals(1, diagnostic.getCandidateCount());
        }
    }

    @Test
    public void diagnosticsAreAbsentForNormalRequests() {
        assertNull(execute(false, "normal", true).getRecallDiagnostics());
    }

    @Test
    public void emptyDisabledAndFailedRecallCannotLookHealthy() {
        RecallDiagnostic empty = execute(true, "empty", true).getRecallDiagnostics().get(1);
        assertEquals("SUCCESS", empty.getStatus());
        assertEquals(0, empty.getCandidateCount());
        RecallDiagnostic disabled = execute(true, "normal", false).getRecallDiagnostics().get(1);
        assertEquals("DISABLED", disabled.getStatus());
        assertEquals(0, disabled.getCandidateCount());
        RecallDiagnostic failed = execute(true, "failed", true).getRecallDiagnostics().get(1);
        assertEquals("FAILED", failed.getStatus());
        assertEquals(0, failed.getCandidateCount());
    }

    private RecommendRes<?> execute(boolean debug, String mode, boolean open) {
        GraphConfig graph = new GraphConfig();
        NodeConfig<RecallConfig> item = recall("item_cf_i2i", "normal", true);
        NodeConfig<RecallConfig> user = recall("user_cf_u2i", mode, open);
        NodeConfig<Object> collector = new NodeConfig<>();
        collector.setName("collector");
        collector.setClazz(Collector.class.getName());
        collector.setOpen(true);
        collector.setTimeout(1000);
        graph.setNodes(List.of(item, user, collector));
        GraphConfig.NodeEdge first = new GraphConfig.NodeEdge();
        first.setFrom(item.getName());
        first.setTo("collector");
        GraphConfig.NodeEdge second = new GraphConfig.NodeEdge();
        second.setFrom(user.getName());
        second.setTo("collector");
        graph.setEdges(List.of(first, second));
        RecService service = new RecService();
        ReflectionTestUtils.setField(service, "redisService", mock(RedisService.class));
        RecommendReq request = new RecommendReq();
        request.setDebug(debug);
        return service.execute(request, GraphPlan.compile(graph));
    }

    private NodeConfig<RecallConfig> recall(String channel, String mode, boolean open) {
        NodeConfig<RecallConfig> node = new NodeConfig<>();
        node.setName(channel);
        node.setClazz(Recall.class.getName());
        node.setOpen(open);
        node.setTimeout(1000);
        RecallConfig content = new RecallConfig();
        content.setRecallType(channel);
        content.setTableName(mode);
        node.setContent(content);
        return node;
    }

    public static class Recall extends AbstractRecallNode<RecallConfig> {
        public Recall(NodeConfig config) {
            super(config);
        }

        @Override
        public void run(GraphContext context) {
            if (!config.isOpen())
                return;
            if ("failed".equals(tableName())) {
                exportChannel(context, List.of(new ScoreResult("uncommitted-item", 1)));
                throw new IllegalStateException("recall store unavailable");
            }
            exportChannel(context,
                "empty".equals(tableName()) ? List.of() : List.of(new ScoreResult("same-overlapping-item", 1)));
        }
    }

    public static class Collector extends AbstractSyncNode<Object> {
        public Collector(NodeConfig config) {
            super(config);
        }

        @Override
        public void run(GraphContext context) {
            context.setResult(List.of(new ScoreResult("selected-other-item", 1)));
        }
    }
}
