package com.openrec.graph.node;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.openrec.graph.GraphContext;
import com.openrec.graph.config.NodeConfig;
import com.openrec.graph.config.OperationConfig;
import com.openrec.graph.config.RankConfig;
import com.openrec.proto.model.ScoreResult;
import com.openrec.service.rank.RankService;

public class RankFallbackTest {
    @Test
    public void itemFallbackSurvivesLateRankCompletion() throws Exception {
        verifyFallback(false);
    }

    @Test
    public void userFallbackSurvivesLateRankCompletion() throws Exception {
        verifyFallback(true);
    }

    private void verifyFallback(boolean user) throws Exception {
        NodeConfig<RankConfig> rankConfig = new NodeConfig<>();
        rankConfig.setName("rank");
        rankConfig.setOpen(true);
        RankConfig rankContent = new RankConfig();
        rankContent.setSize(10);
        rankConfig.setContent(rankContent);
        Node rank = user ? new com.openrec.graph.node.user.RankNode(rankConfig)
            : new com.openrec.graph.node.item.RankNode(rankConfig);
        RankService service = mock(RankService.class);
        when(service.isOpen()).thenReturn(true);
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        when(service.score(any(), anyList(), anyString(), any(), anyMap(), anyMap())).thenAnswer(call -> {
            started.countDown();
            assertTrue(release.await(5, TimeUnit.SECONDS));
            return Map.of("i", 0.9d);
        });
        ReflectionTestUtils.setField(rank, "rankService", service);
        ScoreResult original = new ScoreResult("i", 0.3d, "hot");
        original.addRecallScore("hot", 0.3d);
        GraphContext context = new GraphContext();
        context.addData(user ? "userCandidates" : "combineItems", List.of(original));
        context.addData("userFeatureMap", Map.of("userId", "u"));
        context.importNodeData(rank);
        Thread worker = new Thread(() -> rank.run(context.forkExecution()));
        worker.start();
        try {
            assertTrue(started.await(3, TimeUnit.SECONDS));
            // The graph engine discards timed-out output; operation sees only recall candidates.
            NodeConfig<OperationConfig> operationConfig = new NodeConfig<>();
            operationConfig.setName("operation");
            OperationConfig operationContent = new OperationConfig();
            operationContent.setOperationName("missing-test-rule");
            operationConfig.setContent(operationContent);
            Node operation = user ? new com.openrec.graph.node.user.OperationNode(operationConfig)
                : new com.openrec.graph.node.item.OperationNode(operationConfig);
            context.importNodeData(operation);
            operation.run(context);
            List<?> result =
                (List<?>)context.extractNodeData(operation).get(user ? "operationUsers" : "operationItems");
            assertEquals(List.of(original), result);
        } finally {
            release.countDown();
            worker.join(5000);
        }
        assertFalse(worker.isAlive());
        assertEquals(0.3d, original.getScore(), 0d);
        assertNull(original.getRankScore());
        assertEquals(Double.valueOf(0.3d), original.getRecallScores().get("hot"));
        List<?> ranked = (List<?>)context.extractNodeData(rank).get(user ? "rankUsers" : "rankItems");
        assertEquals(Double.valueOf(0.9d), ((ScoreResult)ranked.get(0)).getRankScore());
    }
}
