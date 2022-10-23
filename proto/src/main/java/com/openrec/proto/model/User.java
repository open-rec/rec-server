package com.openrec.proto.model;

import lombok.Data;

import java.util.List;

@Data
public class User {

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
