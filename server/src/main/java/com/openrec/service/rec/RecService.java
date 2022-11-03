package com.openrec.service.rec;

import com.openrec.graph.GraphConfig;
import com.openrec.graph.GraphEngine;
import com.openrec.graph.RecTemplate;
import com.openrec.proto.biz.recommend.RecommendReq;
import com.openrec.proto.biz.recommend.RecommendRes;
import com.openrec.proto.model.ScoreResult;
import org.springframework.stereotype.Service;

import java.util.List;


@Service
public class RecService {

    private GraphEngine graphEngine;
    private GraphConfig graphConfig;

    public RecService() {
        this.graphEngine = new GraphEngine();
        this.graphConfig = RecTemplate.toGraphConfig();
    }

    public RecommendRes execute(RecommendReq recommendReq) {
        RecommendRes recommendRes = new RecommendRes();
        graphEngine.prepare(recommendReq);
        graphEngine.buildGraph(graphConfig);
        graphEngine.execGraph();
        recommendRes.setResults(graphEngine.getResult());
        return recommendRes;
    }
}
