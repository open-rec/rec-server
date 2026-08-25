package com.openrec.controller.api;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.openrec.proto.JsonReq;
import com.openrec.proto.JsonRes;
import com.openrec.proto.ProtoCode;
import com.openrec.proto.biz.recommend.RecommendReq;
import com.openrec.proto.biz.recommend.RecommendRes;
import com.openrec.proto.model.Item;
import com.openrec.proto.model.User;
import com.openrec.service.metrics.ApiMetricsService;
import com.openrec.ab.AbExperimentService;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import reactor.core.publisher.Mono;

@Api(tags = "推荐")
@RestController
public class RecommendController {

    @Autowired
    private AbExperimentService abExperimentService;

    @Autowired
    private ApiMetricsService apiMetricsService;

    @ApiOperation("物品推荐接口（兼容路径）")
    @RequestMapping(value = {"/api/recommend"}, method = RequestMethod.POST)
    @ResponseBody
    public Mono<JsonRes<RecommendRes<Item>>> recommend(@RequestBody JsonReq<RecommendReq> recommendReq) {
        return recommendItem(recommendReq);
    }

    @ApiOperation("物品推荐接口")
    @RequestMapping(value = {"/api/recommend/item"}, method = RequestMethod.POST)
    @ResponseBody
    public Mono<JsonRes<RecommendRes<Item>>> recommendItem(@RequestBody JsonReq<RecommendReq> recommendReq) {
        prepareTarget(recommendReq.getBody(), RecommendReq.TARGET_ITEM);
        String experiment = abExperimentService.resolve(recommendReq.getBody());
        return Mono.just(new JsonRes<>(apiMetricsService.recordRecommend(experiment,
            () -> abExperimentService.execute(recommendReq.getBody()))));
    }

    @ApiOperation("用户推荐接口")
    @RequestMapping(value = {"/api/recommend/user"}, method = RequestMethod.POST)
    @ResponseBody
    public Mono<JsonRes<RecommendRes<User>>> recommendUser(@RequestBody JsonReq<RecommendReq> recommendReq) {
        prepareTarget(recommendReq.getBody(), RecommendReq.TARGET_USER);
        return Mono.just(new JsonRes<>(ProtoCode.NOT_IMPLEMENTED, false,
            "user recommendation is not implemented", null));
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
