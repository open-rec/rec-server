package com.openrec.service.rec;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.openrec.aop.TimeCost;
import com.openrec.graph.GraphConfig;
import com.openrec.graph.GraphEngine;
import com.openrec.graph.GraphPlan;
import com.openrec.graph.RecTemplate;
import com.openrec.graph.node.NodeRegistry;
import com.openrec.graph.node.ReflectiveNodeFactory;
import com.openrec.graph.node.NodeStatus;
import com.openrec.graph.trace.GraphTraceContext;
import com.openrec.graph.trace.GraphTraceObserver;
import com.openrec.proto.biz.recommend.RecommendReq;
import com.openrec.proto.biz.recommend.RecommendRes;
import com.openrec.proto.model.ScoreResult;
import com.openrec.service.redis.RedisService;

@Service
public class RecService {

    private final AtomicReference<GraphConfig> itemGraphConfig;
    private final AtomicReference<GraphConfig> userGraphConfig;
    private final AtomicReference<GraphPlan> itemGraphPlan;
    private final AtomicReference<GraphPlan> userGraphPlan;
    private final NodeRegistry nodeRegistry;
    private final GraphTraceObserver graphTraceObserver;

    @Autowired
    private RedisService redisService;

    @Value("${recommend.deadline-ms:1000}")
    private long recommendDeadlineMillis = 1000L;

    public RecService() {
        this("item_graph.json", "user_graph.json", NodeRegistry.builder().fallback(new ReflectiveNodeFactory()).build(),
            GraphTraceObserver.NOOP);
    }

    public RecService(@Value("${serving.graph.item-file:item_graph.json}") String itemGraphFile,
        @Value("${serving.graph.user-file:user_graph.json}") String userGraphFile, NodeRegistry nodeRegistry) {
        this(itemGraphFile, userGraphFile, nodeRegistry, GraphTraceObserver.NOOP);
    }

    @Autowired
    public RecService(@Value("${serving.graph.item-file:item_graph.json}") String itemGraphFile,
        @Value("${serving.graph.user-file:user_graph.json}") String userGraphFile, NodeRegistry nodeRegistry,
        GraphTraceObserver graphTraceObserver) {
        this.nodeRegistry = nodeRegistry;
        this.graphTraceObserver = graphTraceObserver;
        this.itemGraphConfig = new AtomicReference<>(RecTemplate.toGraphConfig(itemGraphFile));
        this.userGraphConfig = new AtomicReference<>(RecTemplate.toGraphConfig(userGraphFile));
        this.itemGraphPlan = new AtomicReference<>(compileGraph(itemGraphConfig.get()));
        this.userGraphPlan = new AtomicReference<>(compileGraph(userGraphConfig.get()));
    }

    @TimeCost
    public RecommendRes execute(RecommendReq recommendReq) {
        return execute(recommendReq,
            RecommendReq.TARGET_USER.equals(recommendReq.getTargetType()) ? userGraphPlan.get() : itemGraphPlan.get());
    }

    public RecommendRes execute(RecommendReq recommendReq, GraphPlan selectedGraphPlan) {
        return execute(recommendReq, selectedGraphPlan, GraphTraceContext.create(null, recommendReq.getTargetType(),
            recommendReq.getScene(), "default", "unknown"));
    }

    public RecommendRes execute(RecommendReq recommendReq, GraphPlan selectedGraphPlan,
        GraphTraceContext traceContext) {
        return execute(recommendReq, selectedGraphPlan, traceContext, false, recommendDeadlineMillis, 0L);
    }

    public void probe(RecommendReq request, GraphPlan plan, String requestId, long deadlineMillis,
        long nodeTimeoutFloorMillis) {
        execute(request, plan,
            GraphTraceContext.create(requestId, request.getTargetType(), request.getScene(), "warmup", "warmup"), true,
            deadlineMillis > 0 ? deadlineMillis : recommendDeadlineMillis, nodeTimeoutFloorMillis);
    }

    private RecommendRes execute(RecommendReq recommendReq, GraphPlan selectedGraphPlan, GraphTraceContext traceContext,
        boolean warmup, long deadlineMillis, long nodeTimeoutFloorMillis) {
        RecommendRes recommendRes = new RecommendRes();
        GraphEngine graphEngine = GraphEngine.getSessionGraphEngine(traceContext, graphTraceObserver);
        graphEngine.prepare(recommendReq);
        graphEngine.setWarmup(warmup);
        if (recommendReq != null && recommendReq.getParams() != null) {
            recommendReq.getParams().forEach(graphEngine::addParam);
        }
        graphEngine.execGraph(selectedGraphPlan, deadlineMillis, nodeTimeoutFloorMillis);
        List<ScoreResult> results = graphEngine.getResult();
        if (results == null) {
            throw new IllegalStateException("recommendation graph produced no result; requestId="
                + traceContext.getRequestId() + "; node statuses=" + graphEngine.getNodeStatuses());
        }
        if (warmup && (results.isEmpty()
            || graphEngine.getNodeStatuses().values().stream().anyMatch(status -> status != NodeStatus.SUCCESS))) {
            throw new IllegalStateException(
                "warmup requires candidates and successful graph nodes; statuses=" + graphEngine.getNodeStatuses());
        }
        recommendRes.setResults(results);
        if (recommendReq.isDebug()) {
            String entity = RecommendReq.TARGET_USER.equals(recommendReq.getTargetType()) ? "user" : "item";
            recommendRes.setDetailInfos(redisService.getVs(
                results.stream().map(i -> String.format(entity + ":{%s}", i.getId())).collect(Collectors.toList())));
        }
        return recommendRes;
    }

    public void replaceGraphConfig(String targetType, GraphConfig newGraphConfig) {
        GraphPlan newGraphPlan = compileGraph(newGraphConfig);
        if (RecommendReq.TARGET_USER.equals(targetType)) {
            userGraphConfig.set(newGraphConfig);
            userGraphPlan.set(newGraphPlan);
        } else {
            itemGraphConfig.set(newGraphConfig);
            itemGraphPlan.set(newGraphPlan);
        }
    }

    public GraphPlan compileGraph(GraphConfig graphConfig) {
        return GraphPlan.compile(graphConfig, nodeRegistry);
    }

    public GraphConfig getGraphConfig(String targetType) {
        return RecommendReq.TARGET_USER.equals(targetType) ? userGraphConfig.get() : itemGraphConfig.get();
    }

    public GraphConfig getGraphConfig() {
        return getGraphConfig(RecommendReq.TARGET_ITEM);
    }
}
