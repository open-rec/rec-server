package com.openrec.controller.api;

import com.openrec.proto.JsonReq;
import com.openrec.proto.JsonRes;
import com.openrec.proto.biz.recommend.RecommendReq;
import com.openrec.proto.biz.recommend.RecommendRes;
import com.openrec.proto.model.Item;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@Api(tags = "推荐")
@RestController
public class RecommendController {

    @ApiOperation("推荐接口")
    @RequestMapping(value = {"/api/recommend"}, method = RequestMethod.POST)
    @ResponseBody
    public Mono<JsonRes<RecommendRes<Item>>> recommend(@RequestBody JsonReq<RecommendReq> recommendReq){
        System.out.println(recommendReq);
        return Mono.just(new JsonRes<>(new RecommendRes<>()));
    }
}
