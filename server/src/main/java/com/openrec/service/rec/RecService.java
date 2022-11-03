package com.openrec.service.rec;

import com.openrec.aop.TimeCost;
import com.openrec.graph.GraphConfig;
import com.openrec.graph.GraphEngine;
import com.openrec.graph.RecTemplate;
import com.openrec.proto.biz.recommend.RecommendReq;
import com.openrec.proto.biz.recommend.RecommendRes;
import org.springframework.stereotype.Service;


@Service
public class RecService {

    private GraphConfig graphConfig;

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
        recommendRes.setResults(graphEngine.getResult());
        return recommendRes;
    }
}
