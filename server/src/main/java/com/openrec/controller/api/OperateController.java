package com.openrec.controller.api;

import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.openrec.proto.JsonReq;
import com.openrec.proto.JsonRes;
import com.openrec.service.operate.OperateService;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import reactor.core.publisher.Mono;

@Api(tags = "运营干预")
@RestController
@RequestMapping("/api/operate")
public class OperateController {

    @Autowired
    private OperateService operateService;

    @ApiOperation("黑名单")
    @RequestMapping(value = {"/blacklist"}, method = RequestMethod.POST)
    @ResponseBody
    public Mono<JsonRes<String>> set(@RequestBody JsonReq<Set<String>> blacklist) {
        operateService.setBlacklist(blacklist.getBody());
        return Mono.just(new JsonRes<>());
    }
}
