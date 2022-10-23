package com.openrec.proto.biz.recommend;

import com.openrec.proto.JsonReq;
import lombok.Data;

import java.util.List;

@Data
public class RecommendReq {

    private String sceneId;
    private int size;
    private String userId;
    private String deviceId;
    private List<String> itemIds;
    private String type;
}
