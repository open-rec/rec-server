package com.openrec.service.rec;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.openrec.aop.TimeCost;
import com.openrec.graph.GraphConfig;
import com.openrec.graph.GraphEngine;
import com.openrec.graph.RecTemplate;
import com.openrec.proto.biz.recommend.RecommendReq;
import com.openrec.proto.biz.recommend.RecommendRes;
import com.openrec.proto.model.ScoreResult;
import com.openrec.service.redis.RedisService;

@Service
public class RecService {

    private GraphConfig graphConfig;

    @Autowired
    private RedisService redisService;

    public RecService() {
        this.graphConfig = RecTemplate.toGraphConfig();
    }

    @TimeCost
    public RecommendRes execute(RecommendReq recommendReq) {
        RecommendRes recommendRes = new RecommendRes();
        GraphEngine graphEngine = GraphEngine.getSessionGraphEngine();
        graphEngine.prepare(recommendReq);
        graphEngine.buildGraph(graphConfig);
        graphEngine.execGraph();
        List<ScoreResult> results = graphEngine.getResult();
        recommendRes.setResults(results);
        if (recommendReq.isDebug()) {
            recommendRes.setDetailInfos(redisService
                .getVs(results.stream().map(i -> String.format("item:{%s}", i.getId())).collect(Collectors.toList())));
        }
        return recommendRes;
    }
}
