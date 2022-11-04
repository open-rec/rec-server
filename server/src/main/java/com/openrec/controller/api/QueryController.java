package com.openrec.controller.api;

import com.openrec.proto.JsonRes;
import com.openrec.proto.model.Item;
import com.openrec.proto.model.ScoreResult;
import com.openrec.proto.model.User;
import com.openrec.service.query.QueryService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;

@Api(tags = "数据查询接口")
@RestController
@RequestMapping("/api/query")
public class QueryController {

    @Autowired
    private QueryService queryService;

    @ApiOperation("查询用户")
    @RequestMapping(value = {"/user/{userId}"}, method = RequestMethod.GET)
    @ResponseBody
    public Mono<JsonRes<User>> getUser(@PathVariable String userId) {
        return Mono.just(new JsonRes<>(queryService.queryUser(userId)));
    }

    @ApiOperation("查询物品")
    @RequestMapping(value = {"/item/{itemId}"}, method = RequestMethod.GET)
    @ResponseBody
    public Mono<JsonRes<Item>> getItem(@PathVariable String itemId) {
        return Mono.just(new JsonRes<>(queryService.queryItem(itemId)));
    }

    @ApiOperation("查询用户事件列表")
    @RequestMapping(value = {"/event/{userId}/{scene}/{type}"}, method = RequestMethod.GET)
    @ResponseBody
    public Mono<JsonRes<List<ScoreResult>>> getEvents(@PathVariable String userId,
                                                      @PathVariable String scene,
                                                      @PathVariable String type) {
        return Mono.just(new JsonRes<>(queryService.queryEvent(userId, scene, type)));
    }
}
