package com.openrec.proto.biz.recommend;

import java.util.List;
import java.util.Map;

import lombok.Data;

@Data
public class RecommendReq {

    private String scene;
    private int size;
    private String userId;
    private String deviceId;
    private List<String> itemIds;
    private String type;
    private boolean debug;
    /** Extensible request attributes used by routing and graph nodes. */
    private Map<String, Object> params;
}
