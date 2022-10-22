package com.openrec.controller;

import com.openrec.proto.HealthRes;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
public class DefaultController {

    @RequestMapping(value = {"/"}, method = RequestMethod.GET)
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public Mono<String> home(){
        return Mono.just("hello, rec server start.");
    }

    @RequestMapping(value = {"/health"}, method = RequestMethod.GET)
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public Mono<HealthRes> health(){
        return Mono.just(new HealthRes());
    }
}
