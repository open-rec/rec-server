package com.openrec.graph.config;

import lombok.Data;

/** Common identity for a recall source: where it is stored and how candidates are attributed. */
@Data
public class RecallConfig {

    private String tableName;
    private String recallType;
}
