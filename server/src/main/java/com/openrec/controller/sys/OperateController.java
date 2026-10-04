package com.openrec.controller.sys;

import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.openrec.proto.JsonReq;
import com.openrec.proto.JsonRes;
import com.openrec.service.operate.OperateService;
import com.openrec.config.BlockingTaskExecutor;

import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import reactor.core.publisher.Mono;

@Tag(name = "运营干预")
@RestController
@RequestMapping("/api/operate")
public class OperateController {

    @Autowired
    private OperateService operateService;

    @Autowired
    private BlockingTaskExecutor blockingTaskExecutor;

    @Operation(summary = "黑名单")
    @RequestMapping(value = {"/blacklist"}, method = RequestMethod.POST)
    @ResponseBody
    public Mono<JsonRes<String>> set(@RequestBody JsonReq<Set<String>> blacklist) {
        return blockingTaskExecutor.submit(() -> {
            operateService.setBlacklist(blacklist.getBody());
            return new JsonRes<>();
        });
    }
}
