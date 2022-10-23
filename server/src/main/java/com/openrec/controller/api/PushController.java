package com.openrec.controller.api;

import com.openrec.proto.JsonReq;
import com.openrec.proto.JsonRes;
import com.openrec.proto.biz.push.EventReq;
import com.openrec.proto.biz.push.ItemReq;
import com.openrec.proto.biz.push.UserReq;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/push")
public class PushController {

    @RequestMapping(value = {"/user"}, method = RequestMethod.POST)
    @ResponseBody
    public Mono<JsonRes<String>> pushUser(@RequestBody JsonReq<UserReq> userReq){
        System.out.println(userReq);
        return Mono.just(new JsonRes<>());
    }

    @RequestMapping(value = {"/item"}, method = RequestMethod.POST)
    @ResponseBody
    public Mono<JsonRes<String>> pushItem(@RequestBody JsonReq<ItemReq> itemReq){
        System.out.println(itemReq);
        return Mono.just(new JsonRes<>());
    }

    @RequestMapping(value = {"/event"}, method = RequestMethod.POST)
    @ResponseBody
    public Mono<JsonRes<String>> pushEvent(@RequestBody JsonReq<EventReq> eventReq){
        System.out.println(eventReq);
        return Mono.just(new JsonRes<>());
    }
}
