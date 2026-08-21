package com.openrec.service.metrics;

import java.util.Arrays;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.openrec.proto.biz.push.ItemReq;
import com.openrec.proto.biz.recommend.RecommendRes;
import com.openrec.proto.model.Item;
import com.openrec.proto.model.ScoreResult;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

public class ApiMetricsServiceTest {

    private SimpleMeterRegistry registry;
    private ApiMetricsService metrics;

    @Before
    public void setUp() {
        registry = new SimpleMeterRegistry();
        metrics = new ApiMetricsService(registry);
    }

    @Test
    public void recordsPushTrafficAndPayloadSize() {
        ItemReq request = new ItemReq();
        request.setData(Arrays.asList(new Item(), new Item()));

        metrics.recordPush("item", request, () -> { });

        Assert.assertEquals(1.0, registry.get("openrec_api_requests").tag("type", "item").counter().count(), 0);
        Assert.assertEquals(2.0, registry.get("openrec_push_payload_records").tag("type", "item").summary().totalAmount(), 0);
        Assert.assertTrue(registry.get("openrec_push_payload_bytes").tag("type", "item").summary().totalAmount() > 0);
    }

    @Test
    public void recordsRecommendResultsAndErrors() {
        RecommendRes<Item> response = new RecommendRes<>(Arrays.asList(new ScoreResult(), new ScoreResult()));
        metrics.recordRecommend(() -> response);
        try {
            metrics.recordRecommend(() -> { throw new IllegalStateException("failed"); });
            Assert.fail("expected recommendation failure");
        } catch (IllegalStateException expected) {
            // expected
        }

        Assert.assertEquals(2.0, registry.get("openrec_recommend_result_items").summary().totalAmount(), 0);
        Assert.assertEquals(1.0, registry.get("openrec_api_errors").tag("type", "recommend").counter().count(), 0);
        Assert.assertEquals(2L, registry.get("openrec_api_latency").tag("type", "recommend").timer().count());
    }
}
