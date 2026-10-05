package com.openrec.controller.sys;

import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import com.openrec.proto.biz.recommend.RecommendReq;
import com.openrec.service.rec.RecommendationReadiness;
import com.openrec.config.BlockingTaskExecutor;
import reactor.core.publisher.Mono;

@RestController
public class RecommendationReadinessController {
    private final RecommendationReadiness readiness;
    private final BlockingTaskExecutor executor;
    private final String token;

    public RecommendationReadinessController(RecommendationReadiness readiness, BlockingTaskExecutor executor,
        @Value("${serving.graph.token:openrec-serving-graph-token-change-me}") String token) {
        this.readiness = readiness;
        this.executor = executor;
        this.token = token;
    }

    @GetMapping("/ready")
    public ResponseEntity<Map<String, Object>> ready() {
        Map<String, Object> status = readiness.status();
        return ResponseEntity
            .status(Boolean.TRUE.equals(status.get("ready")) ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE)
            .body(status);
    }

    @PostMapping("/internal/recommendation-warmup")
    public Mono<ResponseEntity<Map<String, Object>>> warmup(
        @RequestHeader(value = "X-OpenRec-Token", required = false) String requestToken,
        @RequestBody(required = false) List<RecommendReq> requests) {
        if (!token.equals(requestToken))
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "invalid serving graph token");
        return executor.submit(() -> {
            readiness.start(requests);
            return ResponseEntity.accepted().body(readiness.status());
        }).onErrorMap(IllegalArgumentException.class,
            error -> new ResponseStatusException(HttpStatus.BAD_REQUEST, error.getMessage(), error));
    }
}
