package com.openrec.service.rec;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.openrec.ab.AbExperimentService;
import com.openrec.graph.GraphConfig;
import com.openrec.graph.GraphPlan;
import com.openrec.proto.biz.recommend.RecommendReq;
import lombok.extern.slf4j.Slf4j;

/** Liveness is independent: ingestion and control APIs must work before recommendation readiness. */
@Slf4j
@Service
public class RecommendationReadiness {
    private final AbExperimentService experiments;
    private final RecService recommendations;
    private final long warmupDeadline;
    private final long warmupNodeBudget;
    private final int requiredSuccesses;
    private final int maxAttempts;
    private final String userId;
    private final String scene;
    private final ScheduledExecutorService worker = Executors.newSingleThreadScheduledExecutor(task -> {
        Thread thread = new Thread(task, "recommendation-warmup");
        thread.setDaemon(true);
        return thread;
    });
    private volatile String state = "WAITING_FOR_SAMPLES";
    private volatile String lastError = "";
    private volatile int attempts;
    private volatile int consecutiveSuccesses;
    private volatile List<RecommendReq> samples = List.of();
    private volatile Map<String, GraphConfig> readyGraphs = Map.of();
    private Map<String, GraphConfig> probedGraphs = Map.of();
    private Map<String, GraphPlan> probedPlans = Map.of();
    private boolean warmed;

    public RecommendationReadiness(AbExperimentService experiments, RecService recommendations,
        @Value("${recommend.warmup.deadline-ms:10000}") long warmupDeadline,
        @Value("${recommend.warmup.node-timeout-ms:2000}") long warmupNodeBudget,
        @Value("${recommend.warmup.successes:3}") int requiredSuccesses,
        @Value("${recommend.warmup.max-attempts:20}") int maxAttempts,
        @Value("${recommend.warmup.user-id:}") String userId,
        @Value("${recommend.warmup.scene:scene_0}") String scene) {
        if (warmupDeadline <= 0 || warmupNodeBudget <= 0 || requiredSuccesses < 1
            || maxAttempts < requiredSuccesses + 1) {
            throw new IllegalArgumentException("invalid recommendation warmup budgets");
        }
        this.experiments = experiments;
        this.recommendations = recommendations;
        this.warmupDeadline = warmupDeadline;
        this.warmupNodeBudget = warmupNodeBudget;
        this.requiredSuccesses = requiredSuccesses;
        this.maxAttempts = maxAttempts;
        this.userId = userId;
        this.scene = scene;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void initialize() {
        if (!userId.isBlank()) {
            RecommendReq sample = new RecommendReq();
            sample.setUserId(userId);
            sample.setScene(scene);
            sample.setSize(12);
            sample.setType("click");
            sample.setParams(new LinkedHashMap<>(Map.of("ab", "default", "query", "item")));
            start(List.of(sample));
        }
        worker.scheduleWithFixedDelay(this::tick, 0, 1, TimeUnit.SECONDS);
    }

    /** Authenticated callers provide representative samples for every target/experiment they serve. */
    public synchronized void start(List<RecommendReq> requests) {
        List<RecommendReq> next = requests == null ? samples : requests;
        if (next.isEmpty() || next.size() > 16) {
            throw new IllegalArgumentException("provide 1 to 16 representative warmup requests");
        }
        for (RecommendReq request : next) {
            if (request == null || request.getScene() == null || request.getScene().isBlank()
                || request.getUserId() == null || request.getUserId().isBlank() || request.getSize() < 1
                || request.getSize() > 100
                || !("item".equals(request.getTargetType()) || "user".equals(request.getTargetType()))) {
                throw new IllegalArgumentException("warmup requires scene, userId, targetType and size (1..100)");
            }
            request.setDebug(false);
            Map<String, Object> params = new LinkedHashMap<>();
            if (request.getParams() != null)
                params.putAll(request.getParams());
            params.put("targetType", request.getTargetType());
            request.setParams(params);
        }
        samples = List.copyOf(next);
        readyGraphs = Map.of();
        probedGraphs = Map.of();
        probedPlans = Map.of();
        warmed = false;
        attempts = 0;
        consecutiveSuccesses = 0;
        lastError = "";
        state = "WARMING";
    }

    private String key(RecommendReq request) {
        return request.getTargetType() + ":" + experiments.resolve(request);
    }

    private GraphConfig graph(RecommendReq request) {
        return experiments.graph(request.getTargetType(), experiments.resolve(request));
    }

    public boolean isReady() {
        return "READY".equals(state) && !samples.isEmpty()
            && samples.stream().allMatch(sample -> readyGraphs.get(key(sample)) == graph(sample));
    }

    public void requireReady(RecommendReq request) {
        requireReady(request, graph(request));
    }

    public void requireReady(RecommendReq request, GraphConfig selectedGraph) {
        if (!isReady() || !readyGraphs.containsKey(key(request)) || readyGraphs.get(key(request)) != selectedGraph) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "recommendation is warming up");
        }
    }

    public Map<String, Object> status() {
        return Map.of("ready", isReady(), "state", "READY".equals(state) && !isReady() ? "STALE" : state, "attempts",
            attempts, "consecutiveSuccesses", consecutiveSuccesses, "requiredSuccesses", requiredSuccesses, "lastError",
            lastError, "warmedRoutes", new ArrayList<>(readyGraphs.keySet()));
    }

    synchronized void tick() {
        if (samples.isEmpty() || "FAILED".equals(state) || isReady())
            return;
        if ("READY".equals(state))
            start(null); // Published graphs and routing changes must be warmed again.
        try {
            attempts++;
            Map<String, GraphConfig> current = new LinkedHashMap<>();
            for (RecommendReq sample : samples)
                current.put(key(sample), graph(sample));
            if (current.size() != probedGraphs.size() || current.entrySet().stream()
                .anyMatch(entry -> probedGraphs.get(entry.getKey()) != entry.getValue())) {
                probedGraphs = current;
                Map<String, GraphPlan> plans = new LinkedHashMap<>();
                current.forEach((key, graph) -> plans.put(key, recommendations.compileGraph(graph)));
                probedPlans = plans;
                warmed = false;
                consecutiveSuccesses = 0;
            }
            state = warmed ? "VERIFYING" : "WARMING";
            for (RecommendReq sample : samples) {
                recommendations.probe(sample, probedPlans.get(key(sample)),
                    "readiness-" + state.toLowerCase() + "-" + attempts + "-" + key(sample),
                    warmed ? 0 : warmupDeadline, warmed ? 0 : warmupNodeBudget);
            }
            if (warmed) {
                consecutiveSuccesses++;
                if (consecutiveSuccesses >= requiredSuccesses
                    && samples.stream().allMatch(sample -> probedGraphs.get(key(sample)) == graph(sample))) {
                    readyGraphs = Map.copyOf(probedGraphs);
                    state = "READY";
                }
            } else {
                warmed = true;
                state = "VERIFYING";
            }
            lastError = "";
            log.info("recommendation readiness: {}", status());
        } catch (Exception error) {
            consecutiveSuccesses = 0;
            lastError = error.getMessage() == null ? error.getClass().getSimpleName() : error.getMessage();
            log.warn("recommendation warmup attempt {} failed: {}", attempts, lastError);
        }
        if (!isReady() && attempts >= maxAttempts) {
            state = "FAILED";
            log.error("recommendation readiness failed: {}", status());
        }
    }

    @PreDestroy
    public void close() {
        worker.shutdownNow();
    }
}
