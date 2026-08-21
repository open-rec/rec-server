package com.openrec.proto.biz.push;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Data;

/** Versioned Kafka contract for entity mutations. */
@Data
@AllArgsConstructor
public class EntityMutation<T> implements Serializable {
    private int schemaVersion;
    private String entityType;
    private PushCmd operation;
    private long occurredAt;
    private T data;

    public static <T> EntityMutation<T> of(String type, PushCmd operation, T data) {
        return new EntityMutation<>(1, type, operation, System.currentTimeMillis(), data);
    }
}
