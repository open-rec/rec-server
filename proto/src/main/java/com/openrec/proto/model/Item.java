package com.openrec.proto.model;

import lombok.Data;

@Data
public class Item {
    private String id;
    private int weight;
    private String title;
    private String category;
    private String tags;
    private String scene;
    private String pubTime;
    private String modifyTime;
    private String expireTime;
    private int status;
    private Object extFields;
}
