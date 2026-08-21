package com.openrec.controller.sys;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import com.openrec.proto.JsonRes;
import com.openrec.service.rec.ServingGraphService;

import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/internal/serving-graph")
public class ServingGraphController {

    @Autowired
    private ServingGraphService servingGraphService;

    @Value("${serving.graph.token:openrec-serving-graph-token-change-me}")
    private String token;

    @PostMapping
    public Mono<JsonRes<Map<String, Object>>> publish(
        @RequestHeader(value = "X-OpenRec-Token", required = false) String requestToken,
        @RequestHeader(value = "X-Graph-Version", required = false) String version,
        @RequestBody String graphJson) {
        authorize(requestToken);
        try {
            return Mono.just(new JsonRes<>(servingGraphService.activate(graphJson, version)));
        } catch (IllegalArgumentException error) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, error.getMessage(), error);
        }
    }

    @GetMapping
    public Mono<JsonRes<Map<String, Object>>> status(
        @RequestHeader(value = "X-OpenRec-Token", required = false) String requestToken) {
        authorize(requestToken);
        return Mono.just(new JsonRes<>(servingGraphService.status()));
    }

    private void authorize(String requestToken) {
        if (requestToken == null || !token.equals(requestToken)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "invalid serving graph token");
        }
    }
}
