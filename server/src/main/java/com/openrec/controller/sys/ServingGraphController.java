package com.openrec.controller.sys;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import com.openrec.proto.JsonRes;
import com.openrec.service.rec.ServingGraphService;
import com.openrec.ab.AbExperimentService;

import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/internal/serving-graph")
public class ServingGraphController {

    @Autowired
    private ServingGraphService servingGraphService;

    @Autowired
    private AbExperimentService abExperimentService;

    @Value("${serving.graph.token:openrec-serving-graph-token-change-me}")
    private String token;

    @PostMapping
    public Mono<JsonRes<Map<String, Object>>> publish(
        @RequestHeader(value = "X-OpenRec-Token", required = false) String requestToken,
        @RequestHeader(value = "X-Graph-Version", required = false) String version,
        @RequestHeader(value = "X-Ab-Experiment", defaultValue = "default") String experiment,
        @RequestBody String graphJson) {
        authorize(requestToken);
        try {
            return Mono.just(new JsonRes<>(servingGraphService.activate(experiment, graphJson, version)));
        } catch (IllegalArgumentException error) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, error.getMessage(), error);
        }
    }

    @GetMapping
    public Mono<JsonRes<Map<String, Object>>> status(
        @RequestHeader(value = "X-OpenRec-Token", required = false) String requestToken,
        @RequestParam(value = "experiment", required = false) String experiment) {
        authorize(requestToken);
        return Mono.just(new JsonRes<>(experiment == null ? servingGraphService.status()
            : servingGraphService.status(experiment)));
    }

    @PutMapping("/routing")
    public Mono<JsonRes<Map<String, Object>>> routing(
        @RequestHeader(value = "X-OpenRec-Token", required = false) String requestToken,
        @RequestBody AbExperimentService.RoutingConfig routing) {
        authorize(requestToken);
        try {
            return Mono.just(new JsonRes<>(abExperimentService.configureRouting(routing)));
        } catch (IllegalArgumentException error) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, error.getMessage(), error);
        }
    }

    @PostMapping("/experiments")
    public Mono<JsonRes<Map<String, Object>>> createExperiment(
        @RequestHeader(value = "X-OpenRec-Token", required = false) String requestToken,
        @RequestBody ExperimentRequest request) {
        authorize(requestToken);
        return lifecycle(() -> abExperimentService.create(request.getName()));
    }

    @PutMapping("/experiments/{experiment}/enabled")
    public Mono<JsonRes<Map<String, Object>>> setExperimentEnabled(
        @RequestHeader(value = "X-OpenRec-Token", required = false) String requestToken,
        @PathVariable String experiment, @RequestBody ExperimentState request) {
        authorize(requestToken);
        return lifecycle(() -> abExperimentService.setEnabled(experiment, request.isEnabled()));
    }

    @DeleteMapping("/experiments/{experiment}")
    public Mono<JsonRes<Map<String, Object>>> deleteExperiment(
        @RequestHeader(value = "X-OpenRec-Token", required = false) String requestToken,
        @PathVariable String experiment) {
        authorize(requestToken);
        return lifecycle(() -> abExperimentService.delete(experiment));
    }

    private Mono<JsonRes<Map<String, Object>>> lifecycle(java.util.function.Supplier<Map<String, Object>> action) {
        try {
            return Mono.just(new JsonRes<>(action.get()));
        } catch (IllegalArgumentException error) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, error.getMessage(), error);
        }
    }

    public static class ExperimentRequest {
        private String name;
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
    }

    public static class ExperimentState {
        private boolean enabled;
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
    }

    private void authorize(String requestToken) {
        if (requestToken == null || !token.equals(requestToken)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "invalid serving graph token");
        }
    }
}
