package com.openrec.proto.model;

import lombok.Data;

@Data
public class Event {

    private String userId;
    private String deviceId;
    private String itemId;
    private String traceId;
    private String type;
    private String value;
    private String time;
    private boolean isLogin;
    private Object extFields;
}
