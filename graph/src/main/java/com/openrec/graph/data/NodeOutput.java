package com.openrec.graph.data;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Immutable output value committed atomically after successful completion. */
public final class NodeOutput {
    private static final NodeOutput EMPTY = new NodeOutput(Collections.emptyMap(), null, false);
    private final Map<DataKey<?>, Object> values;
    private final Object result;
    private final boolean hasResult;

    private NodeOutput(Map<DataKey<?>, Object> values, Object result, boolean hasResult) {
        this.values = Collections.unmodifiableMap(new LinkedHashMap<>(values));
        this.result = result;
        this.hasResult = hasResult;
    }

    public static NodeOutput empty() {
        return EMPTY;
    }

    public static Builder builder() {
        return new Builder();
    }

    public Map<DataKey<?>, Object> values() {
        return values;
    }

    public Object result() {
        return result;
    }

    public boolean hasResult() {
        return hasResult;
    }

    public static final class Builder {
        private final Map<DataKey<?>, Object> values = new LinkedHashMap<>();
        private Object result;
        private boolean hasResult;

        public <T> Builder put(DataKey<T> key, T value) {
            if (value == null)
                throw new IllegalArgumentException("node output cannot be null: " + key);
            key.getType().cast(value);
            values.put(key, freeze(value));
            return this;
        }

        private static Object freeze(Object value) {
            if (value instanceof List)
                return Collections.unmodifiableList(new ArrayList<>((List<?>)value));
            if (value instanceof Set)
                return Collections.unmodifiableSet(new LinkedHashSet<>((Set<?>)value));
            if (value instanceof Map)
                return Collections.unmodifiableMap(new LinkedHashMap<>((Map<?, ?>)value));
            return value;
        }

        public Builder result(Object value) {
            result = value;
            hasResult = true;
            return this;
        }

        public NodeOutput build() {
            return new NodeOutput(values, result, hasResult);
        }
    }
}
