package com.openrec.graph.data;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Immutable input snapshot visible to one node invocation. */
public final class NodeInput {
    private final Map<DataKey<?>, Object> values;

    public NodeInput(Map<DataKey<?>, Object> values) {
        this.values = Collections.unmodifiableMap(new LinkedHashMap<>(values));
    }

    public <T> T require(DataKey<T> key) {
        T value = get(key);
        if (value == null)
            throw new IllegalStateException("missing node input: " + key);
        return value;
    }

    public <T> T get(DataKey<T> key) {
        Object value = values.get(key);
        return value == null ? null : key.getType().cast(value);
    }

    public int size() {
        return values.size();
    }
}
