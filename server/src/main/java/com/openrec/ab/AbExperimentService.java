package com.openrec.ab;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.openrec.graph.GraphConfig;
import com.openrec.graph.GraphPlan;
import com.openrec.proto.biz.recommend.RecommendReq;
import com.openrec.proto.biz.recommend.RecommendRes;
import com.openrec.service.rec.RecService;

/** Owns the independently deployable serving graphs and selects one for each request. */
@Service
public class AbExperimentService {

    public static final String DEFAULT_EXPERIMENT = "default";
    public static final String DEFAULT_ROUTE_PARAM = "ab";
    private static final Pattern EXPERIMENT_NAME = Pattern.compile("[A-Za-z][A-Za-z0-9_]{0,63}");

    private final RecService recService;
    private final ConcurrentMap<String, Experiment> experiments = new ConcurrentHashMap<>();
    private final AtomicReference<RoutingConfig> routing = new AtomicReference<>(new RoutingConfig());

    @Autowired
    public AbExperimentService(RecService recService) {
        this.recService = recService;
        experiments.put(DEFAULT_EXPERIMENT, new Experiment(recService.getGraphConfig(),
            GraphPlan.compile(recService.getGraphConfig()), "classpath-default", null, Instant.now().toString(), true));
    }

    public <T> RecommendRes<T> execute(RecommendReq request) {
        return recService.execute(request, select(request).plan);
    }

    public String resolve(RecommendReq request) {
        RoutingConfig config = routing.get();
        Object routeValue = request == null || request.getParams() == null ? null
            : request.getParams().get(config.getParam());
        String value = routeValue == null ? "" : String.valueOf(routeValue).trim();
        String candidate = config.getRoutes().get(value);
        if (StringUtils.isBlank(candidate) && isRoutable(value)) candidate = value;
        return isRoutable(candidate) ? candidate : config.getDefaultExperiment();
    }

    public GraphConfig graph(String experiment) {
        Experiment item = experiments.get(normalize(experiment));
        return item == null ? required(DEFAULT_EXPERIMENT).graph : item.graph;
    }

    public void activate(String experiment, GraphConfig graph, String version, String checksum) {
        String name = normalize(experiment);
        GraphPlan plan = GraphPlan.compile(graph);
        Experiment previous = experiments.get(name);
        boolean enabled = DEFAULT_EXPERIMENT.equals(name) || previous != null && previous.enabled;
        experiments.put(name, new Experiment(graph, plan, version, checksum, Instant.now().toString(), enabled));
        if (DEFAULT_EXPERIMENT.equals(name)) recService.replaceGraphConfig(graph);
    }

    public Map<String, Object> create(String requestedName) {
        String name = validateName(requestedName);
        if (DEFAULT_EXPERIMENT.equals(name)) throw new IllegalArgumentException("default experiment already exists");
        Experiment source = required(DEFAULT_EXPERIMENT);
        Experiment created = new Experiment(source.graph, source.plan, "draft", source.checksum,
            Instant.now().toString(), false);
        if (experiments.putIfAbsent(name, created) != null) {
            throw new IllegalArgumentException("experiment already exists: " + name);
        }
        return status(name);
    }

    public Map<String, Object> setEnabled(String experiment, boolean enabled) {
        String name = validateName(experiment);
        if (DEFAULT_EXPERIMENT.equals(name) && !enabled) {
            throw new IllegalArgumentException("default experiment cannot be disabled");
        }
        experiments.compute(name, (key, item) -> {
            if (item == null) throw new IllegalArgumentException("experiment does not exist: " + name);
            if (enabled && "draft".equals(item.version)) {
                throw new IllegalArgumentException("experiment graph must be published before enabling: " + name);
            }
            return item.withEnabled(enabled);
        });
        return status(name);
    }

    public Map<String, Object> delete(String experiment) {
        String name = validateName(experiment);
        if (DEFAULT_EXPERIMENT.equals(name)) throw new IllegalArgumentException("default experiment cannot be deleted");
        if (experiments.remove(name) == null) throw new IllegalArgumentException("experiment does not exist: " + name);
        RoutingConfig current = routing.get();
        Map<String, String> routes = new LinkedHashMap<>();
        current.getRoutes().forEach((value, target) -> { if (!name.equals(target)) routes.put(value, target); });
        routing.set(new RoutingConfig(current.getParam(), current.getDefaultExperiment(), routes));
        return status();
    }

    public Map<String, Object> status(String experiment) {
        String name = normalize(experiment);
        Experiment item = experiments.get(name);
        if (item == null) item = required(DEFAULT_EXPERIMENT);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("experiment", name);
        result.put("version", item.version);
        result.put("checksum", item.checksum);
        result.put("loadedAt", item.loadedAt);
        result.put("enabled", item.enabled);
        result.put("graph", item.graph);
        return result;
    }

    public Map<String, Object> status() {
        Map<String, Object> result = status(DEFAULT_EXPERIMENT);
        Map<String, Object> all = new LinkedHashMap<>();
        experiments.keySet().stream().sorted().forEach(name -> all.put(name, status(name)));
        result.put("experiments", all);
        result.put("routing", routing());
        return result;
    }

    public Map<String, Object> routing() {
        RoutingConfig value = routing.get();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("param", value.getParam());
        result.put("defaultExperiment", value.getDefaultExperiment());
        result.put("routes", value.getRoutes());
        return result;
    }

    public Map<String, Object> configureRouting(RoutingConfig requested) {
        if (requested == null) throw new IllegalArgumentException("routing config is required");
        String param = StringUtils.defaultIfBlank(requested.getParam(), DEFAULT_ROUTE_PARAM).trim();
        String fallback = normalize(requested.getDefaultExperiment());
        if (!isRoutable(fallback)) {
            throw new IllegalArgumentException("default experiment does not exist: " + fallback);
        }
        Map<String, String> routes = new LinkedHashMap<>();
        if (requested.getRoutes() != null) {
            requested.getRoutes().forEach((value, experiment) -> {
                if (StringUtils.isBlank(value) || !experiments.containsKey(experiment)) {
                    throw new IllegalArgumentException("route references an unknown experiment: " + experiment);
                }
                routes.put(value.trim(), experiment);
            });
        }
        routing.set(new RoutingConfig(param, fallback, routes));
        return routing();
    }

    private Experiment select(RecommendReq request) {
        return required(resolve(request));
    }

    private boolean isRoutable(String experiment) {
        if (experiment == null) return false;
        Experiment item = experiments.get(experiment);
        return item != null && item.enabled;
    }

    private Experiment required(String experiment) {
        Experiment item = experiments.get(normalize(experiment));
        if (item == null) throw new IllegalArgumentException("experiment does not exist: " + experiment);
        return item;
    }

    private static String normalize(String experiment) {
        return StringUtils.defaultIfBlank(experiment, DEFAULT_EXPERIMENT).trim();
    }

    private static String validateName(String experiment) {
        String name = normalize(experiment);
        if (!EXPERIMENT_NAME.matcher(name).matches()) {
            throw new IllegalArgumentException("invalid experiment name: " + name);
        }
        return name;
    }

    private static final class Experiment {
        private final GraphConfig graph;
        private final GraphPlan plan;
        private final String version;
        private final String checksum;
        private final String loadedAt;
        private final boolean enabled;

        private Experiment(GraphConfig graph, GraphPlan plan, String version, String checksum, String loadedAt,
            boolean enabled) {
            this.graph = graph;
            this.plan = plan;
            this.version = version;
            this.checksum = checksum;
            this.loadedAt = loadedAt;
            this.enabled = enabled;
        }

        private Experiment withEnabled(boolean value) {
            return new Experiment(graph, plan, version, checksum, loadedAt, value);
        }
    }

    public static class RoutingConfig {
        private String param = DEFAULT_ROUTE_PARAM;
        private String defaultExperiment = DEFAULT_EXPERIMENT;
        private Map<String, String> routes = Collections.emptyMap();

        public RoutingConfig() { }
        private RoutingConfig(String param, String defaultExperiment, Map<String, String> routes) {
            this.param = param;
            this.defaultExperiment = defaultExperiment;
            this.routes = Collections.unmodifiableMap(new LinkedHashMap<>(routes));
        }
        public String getParam() { return param; }
        public void setParam(String param) { this.param = param; }
        public String getDefaultExperiment() { return defaultExperiment; }
        public void setDefaultExperiment(String defaultExperiment) { this.defaultExperiment = defaultExperiment; }
        public Map<String, String> getRoutes() { return routes; }
        public void setRoutes(Map<String, String> routes) { this.routes = routes; }
    }
}
