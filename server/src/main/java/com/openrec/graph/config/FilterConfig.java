package com.openrec.graph.config;

import lombok.Data;

import java.util.Map;

@Data
public class FilterConfig {

    private Map<String, TypeFilterConfig> filterMap;

    @Data
    public static class TypeFilterConfig {
        private int duration;
        private int size;
    }
}
