package com.openrec.graph;

public interface RecParams {

    String SCENE = "scene";
    String SIZE = "size";
    String USER_ID = "userId";
    String DEVICE_ID = "deviceId";
    String SESSION_ID = "sessionId";
    String ITEM_IDS = "itemIds";
    String TYPE = "type";
    /** Natural-language request query used by sparse/full-text recall. */
    String QUERY = "query";
    /** Dense representation of the current request query. */
    String QUERY_EMBEDDING = "queryEmbedding";
}
