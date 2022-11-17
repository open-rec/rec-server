package com.openrec.graph;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Maps;

public class GraphParams {

    private Map<String, Object> params;

    public GraphParams() {
        this.params = Maps.newHashMap();
    }

    public String getValueToString(String key) {
        return (String)params.getOrDefault(key, "");
    }

    public int getValueToInt(String key) {
        return (int)params.getOrDefault(key, 0);
    }

    public boolean getValueToBool(String key) {
        return (boolean)params.getOrDefault(key, false);
    }

    public double getValueToDouble(String key) {
        return (double)params.getOrDefault(key, 0d);
    }

    public <T> List<T> getValueToList(String key) {
        return (List<T>)params.getOrDefault(key, null);
    }

    public <T> Set<T> getValueToSet(String key) {
        return (Set<T>)params.getOrDefault(key, null);
    }

    public <K, V> Map<K, V> getValueToMap(String key) {
        return (Map<K, V>)params.getOrDefault(key, null);
    }

    public void put(String key, Object value) {
        params.put(key, value);
    }

    public int size() {
        return params.size();
    }

    public void clear() {
        params.clear();
    }
}
