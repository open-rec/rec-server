package com.openrec.service.rec;

import java.util.List;
import java.util.Map;
import org.junit.After;
import org.junit.Test;
import org.springframework.web.server.ResponseStatusException;
import com.openrec.ab.AbExperimentService;
import com.openrec.graph.GraphConfig;
import com.openrec.graph.GraphPlan;
import com.openrec.proto.biz.recommend.RecommendReq;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

public class RecommendationReadinessTest {
    private final AbExperimentService experiments = mock(AbExperimentService.class);
    private final RecService service = mock(RecService.class);
    private final RecommendationReadiness readiness =
        new RecommendationReadiness(experiments, service, 10000, 2000, 3, 8, "", "s");
    private final GraphConfig graph = new GraphConfig();
    private final GraphPlan plan = mock(GraphPlan.class);

    private RecommendReq sample() {
        RecommendReq request = new RecommendReq();
        request.setUserId("u");
        request.setScene("s");
        request.setSize(12);
        when(experiments.resolve(any())).thenReturn("default");
        when(experiments.graph("item", "default")).thenReturn(graph);
        when(service.compileGraph(any())).thenReturn(plan);
        return request;
    }

    @After
    public void close() {
        readiness.close();
    }

    @Test
    public void gatesUntilIndependentWarmupAndThreeNormalSuccesses() {
        RecommendReq request = sample();
        assertThrows(ResponseStatusException.class, () -> readiness.requireReady(request));
        readiness.start(List.of(request));
        readiness.tick();
        verify(service).probe(eq(request), eq(plan), anyString(), eq(10000L), eq(2000L));
        for (int i = 0; i < 2; i++) {
            readiness.tick();
            assertFalse(readiness.isReady());
        }
        readiness.tick();
        assertTrue(readiness.isReady());
        verify(service, times(3)).probe(eq(request), eq(plan), anyString(), eq(0L), eq(0L));
        readiness.requireReady(request);
        RecommendReq user = new RecommendReq();
        user.setTargetType("user");
        assertThrows(ResponseStatusException.class, () -> readiness.requireReady(user));
    }

    @Test
    public void normalFailureResetsConsecutiveVerificationAndExhaustionStaysClosed() {
        RecommendReq request = sample();
        readiness.start(List.of(request));
        readiness.tick();
        doNothing().doThrow(new IllegalStateException("timeout")).doNothing().when(service).probe(eq(request), eq(plan),
            anyString(), eq(0L), eq(0L));
        readiness.tick();
        readiness.tick();
        assertEquals(0, readiness.status().get("consecutiveSuccesses"));
        readiness.tick();
        readiness.tick();
        assertFalse(readiness.isReady());
        readiness.tick();
        assertTrue(readiness.isReady());
        readiness.start(null);
        doThrow(new IllegalStateException("dependency down")).when(service).probe(any(), any(), anyString(), anyLong(),
            anyLong());
        for (int i = 0; i < 10; i++)
            readiness.tick();
        assertEquals("FAILED", readiness.status().get("state"));
        assertFalse(readiness.isReady());
        assertEquals(8, readiness.status().get("attempts"));
    }

    @Test
    public void publishingGraphInvalidatesReadinessAndRewarmsWithoutChangingConfig() {
        RecommendReq request = sample();
        readiness.start(List.of(request));
        for (int i = 0; i < 4; i++)
            readiness.tick();
        assertTrue(readiness.isReady());
        when(experiments.graph("item", "default")).thenReturn(new GraphConfig());
        assertFalse(readiness.isReady());
        assertThrows(ResponseStatusException.class, () -> readiness.requireReady(request));
        for (int i = 0; i < 4; i++)
            readiness.tick();
        assertTrue(readiness.isReady());
        verify(service, times(2)).probe(any(), any(), anyString(), eq(10000L), eq(2000L));
    }
}
