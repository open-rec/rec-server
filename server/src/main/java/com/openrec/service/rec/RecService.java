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

    @Autowired
    private RedisService redisService;

    @Value("${recommend.deadline-ms:1000}")
    private long recommendDeadlineMillis = 1000L;

    public RecService() {
        this("item_graph.json", "user_graph.json",
            NodeRegistry.builder().fallback(new ReflectiveNodeFactory()).build());
    }

    @Autowired
    public RecService(@Value("${serving.graph.item-file:item_graph.json}") String itemGraphFile,
        @Value("${serving.graph.user-file:user_graph.json}") String userGraphFile, NodeRegistry nodeRegistry) {
        this.nodeRegistry = nodeRegistry;
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
        RecommendRes recommendRes = new RecommendRes();
        GraphEngine graphEngine = GraphEngine.getSessionGraphEngine();
        graphEngine.prepare(recommendReq);
        if (recommendReq != null && recommendReq.getParams() != null) {
            recommendReq.getParams().forEach(graphEngine::addParam);
        }
        graphEngine.execGraph(selectedGraphPlan, recommendDeadlineMillis);
        List<ScoreResult> results = graphEngine.getResult();
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
