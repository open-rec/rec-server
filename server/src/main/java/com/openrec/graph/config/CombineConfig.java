package com.openrec.graph.config;

import java.util.List;

import lombok.Data;

@Data
public class CombineConfig {

    private int size;
    private boolean checkExpireTime;
    private List<String> recallTypes;
}
