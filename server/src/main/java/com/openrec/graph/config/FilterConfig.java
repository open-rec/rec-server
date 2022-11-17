package com.openrec.graph.config;

import java.util.Map;

import lombok.Data;

@Data
public class FilterConfig {

    private Map<String, TypeFilterConfig> filterMap;

    @Data
    public static class TypeFilterConfig {
        private int duration;
        private int size;
    }
}
