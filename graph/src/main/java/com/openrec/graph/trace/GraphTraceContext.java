package com.openrec.graph.trace;

import java.util.UUID;

/** Immutable request metadata propagated explicitly across graph worker threads. */
public final class GraphTraceContext {
    private final String traceId;
    private final String requestId;
    private final String targetType;
    private final String scene;
    private final String experiment;
    private final String graphVersion;

    public GraphTraceContext(String traceId, String requestId, String targetType, String scene, String experiment,
        String graphVersion) {
        this.traceId = blank(traceId) ? UUID.randomUUID().toString() : traceId;
        this.requestId = value(requestId);
        this.targetType = value(targetType);
        this.scene = value(scene);
        this.experiment = value(experiment);
        this.graphVersion = value(graphVersion);
    }

    public static GraphTraceContext create(String requestId, String targetType, String scene, String experiment,
        String graphVersion) {
        return new GraphTraceContext(null, requestId, targetType, scene, experiment, graphVersion);
    }

    private static boolean blank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static String value(String value) {
        return blank(value) ? "unknown" : value.trim();
    }

    public String getTraceId() {
        return traceId;
    }

    public String getRequestId() {
        return requestId;
    }

    public String getTargetType() {
        return targetType;
    }

    public String getScene() {
        return scene;
    }

    public String getExperiment() {
        return experiment;
    }

    public String getGraphVersion() {
        return graphVersion;
    }
}
