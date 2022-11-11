package com.openrec.controller.api;

import com.openrec.proto.JsonReq;
import com.openrec.proto.JsonRes;
import com.openrec.service.operate.OperateService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.Set;

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
