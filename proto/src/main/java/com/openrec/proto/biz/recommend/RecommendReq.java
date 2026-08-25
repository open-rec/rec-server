package com.openrec.proto.biz.recommend;

import java.util.List;
import java.util.Map;

import lombok.Data;

@Data
public class RecommendReq {

    public static final String TARGET_ITEM = "item";
    public static final String TARGET_USER = "user";

    private String scene;
    private int size;
    private String userId;
    private String deviceId;
    private List<String> itemIds;
    private String type;
    private boolean debug;
    /** Set by the serving endpoint; exposed to graph routing and nodes. */
    private String targetType = TARGET_ITEM;
    /** Extensible request attributes used by routing and graph nodes. */
    private Map<String, Object> params;
}
