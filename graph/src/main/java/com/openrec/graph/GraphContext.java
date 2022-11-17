package com.openrec.graph;

import java.lang.reflect.Field;
import java.util.Map;

import com.google.common.collect.Maps;
import com.openrec.graph.config.NodeConfig;
import com.openrec.graph.node.Node;
import com.openrec.graph.tools.anno.Export;
import com.openrec.graph.tools.anno.Import;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class GraphContext {

    private GraphParams params;
    private Map<String, NodeConfig> configMap;
    private Map<String, Object> dataMap;
    private Object result;

    public GraphContext() {
        this.params = new GraphParams();
        this.configMap = Maps.newHashMap();
        this.dataMap = Maps.newConcurrentMap();
    }

    public void exportNodeData(Node node) {
        for (Field field : node.getClass().getDeclaredFields()) {
            if (field.isAnnotationPresent(Export.class)) {
                Export export = field.getAnnotation(Export.class);
                String key = export.value();
                Object data = null;
                try {
                    field.setAccessible(true);
                    data = field.get(node);
                } catch (Exception e) {
                    log.error("node: {} export field: {} failed", node.getName(), field.getName());
                }
                dataMap.put(key, data);
            }
        }
    }

    public void importNodeData(Node node) {
        for (Field field : node.getClass().getDeclaredFields()) {
            if (field.isAnnotationPresent(Import.class)) {
                Import export = field.getAnnotation(Import.class);
                String key = export.value();
                Object data = dataMap.get(key);
                try {
                    field.setAccessible(true);
                    field.set(node, data);
                } catch (Exception e) {
                    log.error("node: {} import field: {} failed", node.getName(), field.getName());
                }
            }
        }
    }

    public void addData(String key, Object data) {
        dataMap.put(key, data);
    }

    public Object getData(String key) {
        return dataMap.get(key);
    }

    public void addParam(String key, Object value) {
        params.put(key, value);
    }

    public GraphParams getParams() {
        return params;
    }

    public void addConfig(String key, NodeConfig nodeConfig) {
        configMap.put(key, nodeConfig);
    }

    public NodeConfig getConfig(String key) {
        return configMap.get(key);
    }

    public Object getResult() {
        return result;
    }

    public void setResult(Object result) {
        this.result = result;
    }

    public void clean() {
        params.clear();
        configMap.clear();
        dataMap.clear();
    }
}
