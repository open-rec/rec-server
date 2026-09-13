package com.openrec.graph.data;

import java.util.Objects;

/** A stable, runtime-checked key used by typed graph nodes. */
public final class DataKey<T> {
    private final String name;
    private final Class<T> type;

    private DataKey(String name, Class<T> type) {
        if (name == null || name.trim().isEmpty() || type == null)
            throw new IllegalArgumentException("data key requires name and type");
        this.name = name;
        this.type = type;
    }

    public static <T> DataKey<T> of(String name, Class<T> type) {
        return new DataKey<>(name, type);
    }

    public String getName() {
        return name;
    }

    public Class<T> getType() {
        return type;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof DataKey))
            return false;
        DataKey<?> key = (DataKey<?>)other;
        return name.equals(key.name) && type.equals(key.type);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, type);
    }

    @Override
    public String toString() {
        return name + ":" + type.getSimpleName();
    }
}
