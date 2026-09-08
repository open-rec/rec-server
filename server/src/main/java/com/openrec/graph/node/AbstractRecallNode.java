package com.openrec.graph.node;

import java.util.List;

import com.openrec.graph.GraphContext;
import com.openrec.graph.config.NodeConfig;
import com.openrec.graph.config.RecallConfig;
import com.openrec.proto.model.ScoreResult;

/** Shared configuration and dynamic channel export for table-backed recall nodes. */
public abstract class AbstractRecallNode<C extends RecallConfig> extends AbstractSyncNode<C> {

    static final String CHANNEL_PREFIX = "recall:";

    protected AbstractRecallNode(NodeConfig nodeConfig) {
        super(nodeConfig);
        if (nodeConfig != null && nodeConfig.isOpen()) {
            if (config.getContent() == null) {
                throw new IllegalArgumentException(getName() + " requires recall config content");
            }
            tableName();
            recallType();
        }
    }

    protected String tableName() {
        return required("tableName", config.getContent().getTableName());
    }

    protected String recallType() {
        return required("recallType", config.getContent().getRecallType());
    }

    protected void exportChannel(GraphContext context, List<ScoreResult> items) {
        context.addData(CHANNEL_PREFIX + recallType(), items);
    }

    private String required(String field, String value) {
        if (value == null || !value.matches("[A-Za-z0-9_-]+")) {
            throw new IllegalArgumentException(getName() + " requires a valid " + field);
        }
        return value;
    }
}
