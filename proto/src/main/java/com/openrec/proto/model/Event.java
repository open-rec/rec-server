package com.openrec.proto.model;

import java.io.Serializable;

import lombok.Data;

@Data
public class Event implements Serializable {

    /** Stable identity of this action. traceId identifies a recommendation/impression context. */
    private String eventId;
    private String userId;
    private String deviceId;
    private String itemId;
    private String traceId;
    private String scene;
    private String type;
    private String value;
    private String time;
    private boolean isLogin;
    private Object extFields;
}
