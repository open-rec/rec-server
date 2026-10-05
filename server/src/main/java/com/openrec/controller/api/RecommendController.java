package com.openrec.controller.api;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.openrec.proto.JsonReq;
import com.openrec.proto.JsonRes;
import com.openrec.proto.biz.recommend.RecommendReq;
import com.openrec.proto.biz.recommend.RecommendRes;
import com.openrec.proto.model.Item;
import com.openrec.proto.model.User;
import com.openrec.service.metrics.ApiMetricsService;
import com.openrec.ab.AbExperimentService;
import com.openrec.config.BlockingTaskExecutor;
import com.openrec.service.rec.RecommendationReadiness;

import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import reactor.core.publisher.Mono;

@Tag(name = "推荐")
@RestController
public class RecommendController {

    @Autowired
    private RecommendationReadiness readiness;

    @Autowired
    private AbExperimentService abExperimentService;

    @Autowired
    private ApiMetricsService apiMetricsService;

    @Autowired
    private BlockingTaskExecutor blockingTaskExecutor;

    @Operation(summary = "物品推荐接口（兼容路径）")
    @RequestMapping(value = {"/api/recommend"}, method = RequestMethod.POST)
    @ResponseBody
    public Mono<JsonRes<RecommendRes<Item>>> recommend(@RequestBody JsonReq<RecommendReq> recommendReq) {
        return recommendItem(recommendReq);
    }

    @Operation(summary = "物品推荐接口")
    @RequestMapping(value = {"/api/recommend/item"}, method = RequestMethod.POST)
    @ResponseBody
    public Mono<JsonRes<RecommendRes<Item>>> recommendItem(@RequestBody JsonReq<RecommendReq> recommendReq) {
        prepareTarget(recommendReq.getBody(), RecommendReq.TARGET_ITEM);
        readiness.requireReady(recommendReq.getBody());
        String experiment = abExperimentService.resolve(recommendReq.getBody());
        return blockingTaskExecutor.submit(() -> new JsonRes<>(apiMetricsService.recordRecommend(experiment, () -> {
            readiness.requireReady(recommendReq.getBody());
            return abExperimentService.execute(recommendReq.getBody(), recommendReq.getRequestId(),
                selected -> readiness.requireReady(recommendReq.getBody(), selected));
        })));
    }

    @Operation(summary = "用户推荐接口")
    @RequestMapping(value = {"/api/recommend/user"}, method = RequestMethod.POST)
    @ResponseBody
    public Mono<JsonRes<RecommendRes<User>>> recommendUser(@RequestBody JsonReq<RecommendReq> recommendReq) {
        prepareTarget(recommendReq.getBody(), RecommendReq.TARGET_USER);
        readiness.requireReady(recommendReq.getBody());
        String experiment = abExperimentService.resolve(recommendReq.getBody());
        return blockingTaskExecutor.submit(() -> new JsonRes<>(apiMetricsService.recordRecommend(experiment, () -> {
            readiness.requireReady(recommendReq.getBody());
            return abExperimentService.execute(recommendReq.getBody(), recommendReq.getRequestId(),
                selected -> readiness.requireReady(recommendReq.getBody(), selected));
        })));
    }

    private static void prepareTarget(RecommendReq request, String targetType) {
        if (request == null) {
            throw new IllegalArgumentException("recommend request body is required");
        }
        request.setTargetType(targetType);
        if (request.getParams() == null) {
            request.setParams(new java.util.LinkedHashMap<>());
        }
        request.getParams().put("targetType", targetType);
    }
}
