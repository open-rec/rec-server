package com.openrec.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import com.openrec.proto.JsonRes;

import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import reactor.core.publisher.Mono;

@Tag(name = "默认接口")
@RestController
public class DefaultController {

    @Operation(summary = "默认主页")
    @RequestMapping(value = {"/"}, method = RequestMethod.GET)
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public Mono<String> home() {
        return Mono.just("hello, rec server start.");
    }

    @Operation(summary = "健康检查")
    @RequestMapping(value = {"/health"}, method = RequestMethod.GET)
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public Mono<JsonRes<String>> health() {
        return Mono.just(new JsonRes<>("health check"));
    }
}
