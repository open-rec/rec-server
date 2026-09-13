package com.openrec.service.metrics;

import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.openrec.graph.trace.GraphExecutionTrace;
import com.openrec.graph.trace.GraphTraceObserver;
import com.openrec.graph.trace.NodeExecutionTrace;
import com.openrec.util.JsonUtil;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

/** Emits one structured graph trace and bounded-cardinality metrics per execution. */
@Service
public class GraphTraceService implements GraphTraceObserver {
    private static final Logger log = LoggerFactory.getLogger("graph.trace");
    private final MeterRegistry registry;

    public GraphTraceService(MeterRegistry registry) {
        this.registry = registry;
    }

    @Override
    public void onComplete(GraphExecutionTrace trace) {
        String target = trace.getContext().getTargetType();
        String experiment = trace.getContext().getExperiment();
        Timer.builder("openrec_graph_latency").description("Graph execution latency")
            .tags("target", target, "ab", experiment).publishPercentileHistogram().register(registry)
            .record(trace.getDurationMillis(), TimeUnit.MILLISECONDS);
        Counter.builder("openrec_graph_executions").description("Graph executions")
            .tags("target", target, "ab", experiment, "deadline", String.valueOf(trace.isDeadlineExceeded()))
            .register(registry).increment();
        for (NodeExecutionTrace node : trace.getNodes()) {
            String type = safe(node.getNodeType());
            String status = node.getStatus().name();
            Counter.builder("openrec_graph_node_executions").description("Graph node outcomes")
                .tags("target", target, "node", type, "status", status).register(registry).increment();
            Timer.builder("openrec_graph_node_latency").description("Graph node execution latency")
                .tags("target", target, "node", type, "status", status).publishPercentileHistogram().register(registry)
                .record(node.getExecutionMillis(), TimeUnit.MILLISECONDS);
            Timer.builder("openrec_graph_node_queue_latency").description("Graph node queue latency")
                .tags("target", target, "node", type).publishPercentileHistogram().register(registry)
                .record(node.getQueuedMillis(), TimeUnit.MILLISECONDS);
            DistributionSummary.builder("openrec_graph_node_input_items").tags("target", target, "node", type)
                .register(registry).record(node.getInputCount());
            DistributionSummary.builder("openrec_graph_node_output_items").tags("target", target, "node", type)
                .register(registry).record(node.getOutputCount());
        }
        log.info("graph_trace={}", JsonUtil.objToJson(trace));
    }

    private static String safe(String value) {
        return value == null || value.trim().isEmpty() ? "unknown" : value;
    }
}
