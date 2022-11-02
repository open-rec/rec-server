package com.openrec.controller;

import com.openrec.proto.JsonRes;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@Api(tags = "默认接口")
@RestController
public class DefaultController {

    @ApiOperation("默认主页")
    @RequestMapping(value = {"/"}, method = RequestMethod.GET)
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public Mono<String> home() {
        return Mono.just("hello, rec server start.");
    }

    @ApiOperation("健康检查")
    @RequestMapping(value = {"/health"}, method = RequestMethod.GET)
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public Mono<JsonRes<String>> health() {
        return Mono.just(new JsonRes<>("health check"));
    }
}
