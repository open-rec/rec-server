package com.openrec.service.rec;

import java.lang.reflect.Constructor;
import java.security.MessageDigest;
import java.util.*;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.openrec.graph.GraphConfig;
import com.openrec.graph.RecTemplate;
import com.openrec.graph.config.NodeConfig;
import com.openrec.graph.node.Node;
import com.openrec.ab.AbExperimentService;
import com.openrec.util.JsonUtil;

@Service
public class ServingGraphService {

    @Autowired
    private AbExperimentService abExperimentService;

    public Map<String, Object> activate(String graphJson, String requestedVersion) {
        return activate(AbExperimentService.DEFAULT_EXPERIMENT, graphJson, requestedVersion);
    }

    public Map<String, Object> activate(String experiment, String graphJson, String requestedVersion) {
        GraphConfig parsed = RecTemplate.parse(graphJson);
        GraphConfig merged = mergeNodeConfigs(abExperimentService.graph(experiment), parsed);
        validate(merged);
        String canonical = JsonUtil.objToJson(merged);
        abExperimentService.activate(experiment, merged,
            StringUtils.defaultIfBlank(requestedVersion, "graph-" + System.currentTimeMillis()), sha256(canonical));
        return status(experiment);
    }

    private GraphConfig mergeNodeConfigs(GraphConfig current, GraphConfig received) {
        if (received == null || received.getNodes() == null || received.getNodes().isEmpty()) {
            throw new IllegalArgumentException("graph must contain nodes");
        }

        Map<String, NodeConfig> updates = new LinkedHashMap<>();
        for (NodeConfig config : received.getNodes()) {
            if (config == null || StringUtils.isBlank(config.getName())) {
                throw new IllegalArgumentException("every node requires a name");
            }
            if (updates.put(config.getName(), config) != null) {
                throw new IllegalArgumentException("duplicate node: " + config.getName());
            }
        }

        GraphConfig merged = new GraphConfig();
        List<NodeConfig> nodes = new ArrayList<>();
        for (NodeConfig config : current.getNodes()) {
            nodes.add(updates.containsKey(config.getName()) ? updates.get(config.getName()) : config);
        }
        merged.setNodes(nodes);
        merged.setEdges(current.getEdges());
        return merged;
    }

    public Map<String, Object> status() {
        ensureDefaultChecksum();
        return abExperimentService.status();
    }

    public Map<String, Object> status(String experiment) {
        ensureDefaultChecksum();
        return abExperimentService.status(experiment);
    }

    private void ensureDefaultChecksum() {
        Map<String, Object> current = abExperimentService.status(AbExperimentService.DEFAULT_EXPERIMENT);
        if (current.get("checksum") == null) {
            GraphConfig graph = (GraphConfig) current.get("graph");
            abExperimentService.activate(AbExperimentService.DEFAULT_EXPERIMENT, graph,
                String.valueOf(current.get("version")), sha256(JsonUtil.objToJson(graph)));
        }
    }

    private void validate(GraphConfig graph) {
        if (graph == null || graph.getNodes() == null || graph.getNodes().isEmpty()
            || graph.getEdges() == null) {
            throw new IllegalArgumentException("graph must contain nodes and edges");
        }
        Map<String, NodeConfig> nodes = new LinkedHashMap<>();
        for (NodeConfig config : graph.getNodes()) {
            if (config == null || StringUtils.isBlank(config.getName())) {
                throw new IllegalArgumentException("every node requires a name");
            }
            if (nodes.put(config.getName(), config) != null) {
                throw new IllegalArgumentException("duplicate node: " + config.getName());
            }
            if (config.getTimeout() <= 0) {
                throw new IllegalArgumentException("node timeout must be positive: " + config.getName());
            }
            validateNodeClass(config);
        }
        Map<String, Integer> indegree = new HashMap<>();
        Map<String, List<String>> children = new HashMap<>();
        nodes.keySet().forEach(name -> { indegree.put(name, 0); children.put(name, new ArrayList<>()); });
        Set<String> edges = new HashSet<>();
        for (GraphConfig.NodeEdge edge : graph.getEdges()) {
            if (edge == null || !nodes.containsKey(edge.getFrom()) || !nodes.containsKey(edge.getTo())) {
                throw new IllegalArgumentException("edge references an unknown node");
            }
            if (edge.getFrom().equals(edge.getTo()) || !edges.add(edge.getFrom() + "->" + edge.getTo())) {
                throw new IllegalArgumentException("invalid or duplicate edge: " + edge.getFrom() + "->" + edge.getTo());
            }
            children.get(edge.getFrom()).add(edge.getTo());
            indegree.put(edge.getTo(), indegree.get(edge.getTo()) + 1);
        }
        Queue<String> ready = new ArrayDeque<>();
        indegree.forEach((name, degree) -> { if (degree == 0) ready.add(name); });
        int visited = 0;
        while (!ready.isEmpty()) {
            String name = ready.remove(); visited++;
            for (String child : children.get(name)) {
                int degree = indegree.get(child) - 1; indegree.put(child, degree);
                if (degree == 0) ready.add(child);
            }
        }
        if (visited != nodes.size()) throw new IllegalArgumentException("graph contains a cycle");
    }

    private void validateNodeClass(NodeConfig config) {
        try {
            Class<?> clazz = Class.forName(config.getClazz());
            if (!Node.class.isAssignableFrom(clazz)) {
                throw new IllegalArgumentException("node class does not implement Node: " + config.getClazz());
            }
            Constructor<?> constructor = clazz.getDeclaredConstructor(NodeConfig.class);
            constructor.newInstance(config);
        } catch (IllegalArgumentException error) {
            throw error;
        } catch (Exception error) {
            throw new IllegalArgumentException("cannot construct node " + config.getName() + ": " + error.getMessage(), error);
        }
    }

    private static String sha256(String value) {
        try {
            byte[] bytes = MessageDigest.getInstance("SHA-256").digest(value.getBytes("UTF-8"));
            StringBuilder result = new StringBuilder("sha256:");
            for (byte item : bytes) result.append(String.format("%02x", item));
            return result.toString();
        } catch (Exception error) {
            throw new IllegalStateException(error);
        }
    }
}
