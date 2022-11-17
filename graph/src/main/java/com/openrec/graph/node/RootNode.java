package com.openrec.graph.node;

import com.openrec.graph.GraphContext;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RootNode extends SyncNode {

    @Override
    public void run(GraphContext context) {
        log.info("root node start");
    }
}
