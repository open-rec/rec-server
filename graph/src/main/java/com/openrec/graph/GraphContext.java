package com.openrec.graph;

import java.util.Map;
import java.util.HashMap;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Set;

import com.google.common.collect.Maps;
import com.openrec.graph.config.NodeConfig;
import com.openrec.graph.data.DataKey;
import com.openrec.graph.data.NodeInput;
import com.openrec.graph.data.NodeOutput;
import com.openrec.graph.node.Node;
import com.openrec.graph.node.NodeContract;

public class GraphContext {

    private GraphParams params;
    private Map<String, NodeConfig> configMap;
    private Map<String, Object> dataMap;
    private Map<DataKey<?>, Object> typedDataMap;
    private Object result;
    private GraphContext parent;

    public GraphContext() {
        this.params = new GraphParams();
        this.configMap = Maps.newHashMap();
        this.dataMap = Maps.newConcurrentMap();
        this.typedDataMap = Maps.newConcurrentMap();
    }

    private GraphContext(GraphContext parent) {
        this.params = parent.params;
        this.configMap = parent.configMap;
        this.dataMap = Maps.newHashMap();
        this.typedDataMap = Maps.newHashMap();
        this.parent = parent;
    }

    public GraphContext forkExecution() {
        return new GraphContext(this);
    }

    public void exportNodeData(Node node) {
        commitNodeData(extractNodeData(node, NodeContract.inspect(node.getClass())));
    }

    public Map<String, Object> extractNodeData(Node node) {
        return extractNodeData(node, NodeContract.inspect(node.getClass()));
    }

    public Map<String, Object> extractNodeData(Node node, NodeContract contract) {
        Map<String, Object> exported = new HashMap<>();
        for (NodeContract.Port port : contract.exports()) {
            Object data = port.read(node);
            if (data != null)
                exported.put(port.name(), data);
        }
        return exported;
    }

    public void commitNodeData(Map<String, Object> exported) {
        dataMap.putAll(exported);
    }

    public int importNodeData(Node node) {
        return importNodeData(node, NodeContract.inspect(node.getClass()));
    }

    public int importNodeData(Node node, NodeContract contract) {
        int imported = 0;
        for (NodeContract.Port port : contract.imports()) {
            Object data = getData(port.name());
            if (data == null && port.required())
                throw new IllegalStateException("node " + node.getName() + " requires graph input " + port.name());
            port.write(node, data);
            if (data instanceof java.util.Collection)
                imported += ((java.util.Collection<?>)data).size();
            else if (data instanceof Map)
                imported += ((Map<?, ?>)data).size();
            else if (data != null)
                imported++;
        }
        return imported;
    }

    public void addData(String key, Object data) {
        dataMap.put(key, data);
    }

    public Object getData(String key) {
        Object value = dataMap.get(key);
        return value != null || parent == null ? value : parent.getData(key);
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
        return result != null || parent == null ? result : parent.getResult();
    }

    public void setResult(Object result) {
        this.result = result;
    }

    public void commitExecution(GraphContext execution, Map<String, Object> exported) {
        dataMap.putAll(execution.dataMap);
        dataMap.putAll(exported);
        if (execution.result != null)
            result = execution.result;
    }

    public synchronized NodeInput snapshot(Set<DataKey<?>> keys) {
        Map<DataKey<?>, Object> values = new LinkedHashMap<>();
        for (DataKey<?> key : keys) {
            Object value = typedDataMap.get(key);
            if (value != null)
                values.put(key, value);
        }
        return new NodeInput(Collections.unmodifiableMap(values));
    }

    public synchronized void commit(NodeOutput output) {
        typedDataMap.putAll(output.values());
        if (output.hasResult())
            result = output.result();
    }

    public <T> T getData(DataKey<T> key) {
        Object value = typedDataMap.get(key);
        return value == null ? null : key.getType().cast(value);
    }

    public void clean() {
        params.clear();
        configMap.clear();
        dataMap.clear();
        typedDataMap.clear();
    }
}
