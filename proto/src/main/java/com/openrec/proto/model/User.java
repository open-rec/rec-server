package com.openrec.proto.model;

import java.io.Serializable;
import java.util.List;

import lombok.Data;

@Data
public class User implements Serializable {

    private String id;
    private String deviceId;
    private String name;
    private String gender;
    private int age;
    private String country;
    private String city;
    private String phone;
    private List<String> tags;
    private String registerTime;
    private String loginTime;
    private Object extFields;
}
