package com.openrec.controller.api;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.*;

import com.openrec.proto.JsonReq;
import com.openrec.proto.JsonRes;
import com.openrec.proto.biz.push.EventReq;
import com.openrec.proto.biz.push.ItemReq;
import com.openrec.proto.biz.push.UserReq;
import com.openrec.service.push.PushService;
import com.openrec.service.metrics.ApiMetricsService;
import com.openrec.config.BlockingTaskExecutor;

import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import reactor.core.publisher.Mono;

import jakarta.annotation.Resource;

@Tag(name = "数据推送接口")
@RestController
@RequestMapping("/api/push")
public class PushController {

    @Autowired
    @Resource(name = "${server.pushService}")
    private PushService pushService;

    @Autowired
    private ApiMetricsService apiMetricsService;

    @Autowired
    private BlockingTaskExecutor blockingTaskExecutor;

    @Operation(summary = "用户表推送")
    @RequestMapping(value = {"/user"}, method = RequestMethod.POST)
    @ResponseBody
    public Mono<JsonRes<String>> pushUser(@RequestBody JsonReq<UserReq> userReq) {
        return blockingTaskExecutor.submit(() -> {
            apiMetricsService.recordPush("user", userReq.getBody(), () -> pushService.pushUser(userReq.getBody()));
            return new JsonRes<>();
        });
    }

    @Operation(summary = "物品表推送")
    @RequestMapping(value = {"/item"}, method = RequestMethod.POST)
    @ResponseBody
    public Mono<JsonRes<String>> pushItem(@RequestBody JsonReq<ItemReq> itemReq) {
        return blockingTaskExecutor.submit(() -> {
            apiMetricsService.recordPush("item", itemReq.getBody(), () -> pushService.pushItem(itemReq.getBody()));
            return new JsonRes<>();
        });
    }

    @Operation(summary = "事件推送表")
    @RequestMapping(value = {"/event"}, method = RequestMethod.POST)
    @ResponseBody
    public Mono<JsonRes<String>> pushEvent(@RequestBody JsonReq<EventReq> eventReq) {
        return blockingTaskExecutor.submit(() -> {
            apiMetricsService.recordPush("event", eventReq.getBody(), () -> pushService.pushEvent(eventReq.getBody()));
            return new JsonRes<>();
        });
    }
}
