package com.openrec.service.rec;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.openrec.aop.TimeCost;
import com.openrec.graph.GraphConfig;
import com.openrec.graph.GraphEngine;
import com.openrec.graph.GraphPlan;
import com.openrec.graph.RecTemplate;
import com.openrec.proto.biz.recommend.RecommendReq;
import com.openrec.proto.biz.recommend.RecommendRes;
import com.openrec.proto.model.ScoreResult;
import com.openrec.service.redis.RedisService;

@Service
public class RecService {

    private final AtomicReference<GraphConfig> graphConfig;
    private final AtomicReference<GraphPlan> graphPlan;

    @Autowired
    private RedisService redisService;

    public RecService() {
        this.graphConfig = new AtomicReference<>(RecTemplate.toGraphConfig());
        this.graphPlan = new AtomicReference<>(GraphPlan.compile(graphConfig.get()));
    }

    @TimeCost
    public RecommendRes execute(RecommendReq recommendReq) {
        return execute(recommendReq, graphPlan.get());
    }

    public RecommendRes execute(RecommendReq recommendReq, GraphPlan selectedGraphPlan) {
        RecommendRes recommendRes = new RecommendRes();
        GraphEngine graphEngine = GraphEngine.getSessionGraphEngine();
        graphEngine.prepare(recommendReq);
        if (recommendReq != null && recommendReq.getParams() != null) {
            recommendReq.getParams().forEach(graphEngine::addParam);
        }
        graphEngine.execGraph(selectedGraphPlan);
        List<ScoreResult> results = graphEngine.getResult();
        recommendRes.setResults(results);
        if (recommendReq.isDebug()) {
            recommendRes.setDetailInfos(redisService
                .getVs(results.stream().map(i -> String.format("item:{%s}", i.getId())).collect(Collectors.toList())));
        }
        return recommendRes;
    }

    public void replaceGraphConfig(GraphConfig newGraphConfig) {
        GraphPlan newGraphPlan = GraphPlan.compile(newGraphConfig);
        graphConfig.set(newGraphConfig);
        graphPlan.set(newGraphPlan);
    }

    public GraphConfig getGraphConfig() {
        return graphConfig.get();
    }
}
