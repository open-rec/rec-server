package com.openrec.controller.api;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.openrec.proto.JsonReq;
import com.openrec.proto.JsonRes;
import com.openrec.proto.biz.recommend.RecommendReq;
import com.openrec.proto.biz.recommend.RecommendRes;
import com.openrec.proto.model.Item;
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

    @ApiOperation("推荐接口")
    @RequestMapping(value = {"/api/recommend"}, method = RequestMethod.POST)
    @ResponseBody
    public Mono<JsonRes<RecommendRes<Item>>> recommend(@RequestBody JsonReq<RecommendReq> recommendReq) {
        String experiment = abExperimentService.resolve(recommendReq.getBody());
        return Mono.just(new JsonRes<>(apiMetricsService.recordRecommend(experiment,
            () -> abExperimentService.execute(recommendReq.getBody()))));
    }
}
