package com.openrec.service.rec;

import com.google.gson.Gson;
import com.openrec.graph.GraphConfig;
import com.openrec.graph.GraphEngine;
import com.openrec.graph.RecTemplate;
import com.openrec.proto.biz.recommend.RecommendReq;
import com.openrec.proto.biz.recommend.RecommendRes;
import org.springframework.stereotype.Service;


@Service
public class RecService {

    private String graphTemplate;
    private GraphEngine graphEngine;
    private GraphConfig graphConfig;

    public RecService() {
        this.graphEngine = new GraphEngine();
        this.graphTemplate = RecTemplate.load();
        this.graphConfig = new Gson().fromJson(graphTemplate, GraphConfig.class);
    }

    public RecommendRes execute(RecommendReq recommendReq) {
        graphEngine.prepare(recommendReq, graphConfig);
        graphEngine.buildGraph(graphConfig);
        graphEngine.execGraph();
        // TODO: 2022/11/1
        return null;
    }
}
