package com.openrec.controller.api;

import com.openrec.proto.JsonReq;
import com.openrec.proto.JsonRes;
import com.openrec.proto.biz.push.EventReq;
import com.openrec.proto.biz.push.ItemReq;
import com.openrec.proto.biz.push.UserReq;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@Api(tags = "数据推送接口")
@RestController
@RequestMapping("/api/push")
public class PushController {

    @ApiOperation("用户表推送")
    @RequestMapping(value = {"/user"}, method = RequestMethod.POST)
    @ResponseBody
    public Mono<JsonRes<String>> pushUser(@RequestBody JsonReq<UserReq> userReq){
        return Mono.just(new JsonRes<>());
    }

    @ApiOperation("物品表推送")
    @RequestMapping(value = {"/item"}, method = RequestMethod.POST)
    @ResponseBody
    public Mono<JsonRes<String>> pushItem(@RequestBody JsonReq<ItemReq> itemReq){
        return Mono.just(new JsonRes<>());
    }

    @ApiOperation("事件推送表")
    @RequestMapping(value = {"/event"}, method = RequestMethod.POST)
    @ResponseBody
    public Mono<JsonRes<String>> pushEvent(@RequestBody JsonReq<EventReq> eventReq){
        return Mono.just(new JsonRes<>());
    }
}
