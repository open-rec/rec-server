package com.openrec.proto.model;

import lombok.Data;

import java.io.Serializable;

@Data
public class Item implements Serializable {
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
