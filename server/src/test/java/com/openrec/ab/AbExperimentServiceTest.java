package com.openrec.ab;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import com.openrec.proto.biz.recommend.RecommendReq;
import com.openrec.service.rec.RecService;

public class AbExperimentServiceTest {

    @Test
    public void routesByExtensibleParamAndFallsBackToDefault() {
        RecService recService = new RecService();
        AbExperimentService service = new AbExperimentService(recService);
        service.create("test1");
        service.activate("test1", recService.getGraphConfig(), "test-v1", "checksum");
        AbExperimentService.RoutingConfig routing = new AbExperimentService.RoutingConfig();
        routing.setRoutes(Collections.singletonMap("bucket-a", "test1"));
        service.configureRouting(routing);

        RecommendReq request = new RecommendReq();
        Map<String, Object> params = new HashMap<>();
        params.put("ab", "bucket-a");
        request.setParams(params);
        Assert.assertEquals("default", service.resolve(request));
        service.setEnabled("test1", true);
        Assert.assertEquals("test1", service.resolve(request));
        service.setEnabled("test1", false);
        Assert.assertEquals("default", service.resolve(request));
        service.setEnabled("test1", true);
        params.put("ab", "unknown");
        Assert.assertEquals("default", service.resolve(request));
        Assert.assertEquals("default", service.resolve(new RecommendReq()));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsRouteToMissingExperiment() {
        AbExperimentService service = new AbExperimentService(new RecService());
        AbExperimentService.RoutingConfig routing = new AbExperimentService.RoutingConfig();
        routing.setRoutes(Collections.singletonMap("bucket-a", "missing"));
        service.configureRouting(routing);
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsEnablingUnpublishedDraft() {
        AbExperimentService service = new AbExperimentService(new RecService());
        service.create("draft1");
        service.setEnabled("draft1", true);
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsExperimentNameWithHyphen() {
        new AbExperimentService(new RecService()).create("test-1");
    }

    @Test
    public void deletesExperimentFromRuntimeAndRoutes() {
        AbExperimentService service = new AbExperimentService(new RecService());
        service.create("test1");
        service.activate("test1", service.graph("test1"), "test-v1", "checksum");
        service.setEnabled("test1", true);
        AbExperimentService.RoutingConfig routing = new AbExperimentService.RoutingConfig();
        routing.setRoutes(Collections.singletonMap("a", "test1"));
        service.configureRouting(routing);

        service.delete("test1");

        Map<?, ?> experiments = (Map<?, ?>) service.status().get("experiments");
        Assert.assertFalse(experiments.containsKey("test1"));
        Assert.assertTrue(((Map<?, ?>) service.routing().get("routes")).isEmpty());
    }
}
