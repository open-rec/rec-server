package com.openrec.proto.model;

import java.io.Serializable;

import lombok.Data;

/** Optional session metadata; behavioral aggregates are derived from Event.sessionId. */
@Data
public class Session implements Serializable {
    private String id;
    private String userId;
    private String deviceId;
    private String scene;
    private String startTime;
    private String lastEventTime;
    private Object extFields;
}
