package com.openrec.service.rec;

import java.security.MessageDigest;
import java.util.*;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.openrec.graph.GraphConfig;
import com.openrec.graph.RecTemplate;
import com.openrec.graph.config.NodeConfig;
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
        return activate("item", experiment, graphJson, requestedVersion);
    }

    public Map<String, Object> activate(String targetType, String experiment, String graphJson,
        String requestedVersion) {
        GraphConfig parsed = RecTemplate.parse(graphJson);
        GraphConfig merged = mergeNodeConfigs(abExperimentService.graph(targetType, experiment), parsed);
        validate(merged);
        String canonical = JsonUtil.objToJson(merged);
        abExperimentService.activate(targetType, experiment, merged,
            StringUtils.defaultIfBlank(requestedVersion, "graph-" + System.currentTimeMillis()), sha256(canonical));
        return status(targetType, experiment);
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

    public Map<String, Object> status(String targetType, String experiment) {
        ensureDefaultChecksum(targetType);
        return abExperimentService.status(targetType, experiment);
    }

    private void ensureDefaultChecksum() {
        ensureDefaultChecksum("item");
    }

    private void ensureDefaultChecksum(String targetType) {
        Map<String, Object> current = abExperimentService.status(targetType, AbExperimentService.DEFAULT_EXPERIMENT);
        if (current.get("checksum") == null) {
            GraphConfig graph = (GraphConfig)current.get("graph");
            abExperimentService.activate(targetType, AbExperimentService.DEFAULT_EXPERIMENT, graph,
                String.valueOf(current.get("version")), sha256(JsonUtil.objToJson(graph)));
        }
    }

    private void validate(GraphConfig graph) {
        abExperimentService.validate(graph);
    }

    private static String sha256(String value) {
        try {
            byte[] bytes = MessageDigest.getInstance("SHA-256").digest(value.getBytes("UTF-8"));
            StringBuilder result = new StringBuilder("sha256:");
            for (byte item : bytes)
                result.append(String.format("%02x", item));
            return result.toString();
        } catch (Exception error) {
            throw new IllegalStateException(error);
        }
    }
}
