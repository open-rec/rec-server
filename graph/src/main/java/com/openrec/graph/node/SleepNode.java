package com.openrec.graph.node;

import java.util.concurrent.TimeUnit;

import com.openrec.graph.GraphContext;
import com.openrec.graph.config.NodeConfig;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SleepNode extends SyncNode {

    private static final long DEFAULT_SLEEP_TIME = TimeUnit.SECONDS.toMillis(1);

    private long sleetTime;

    public SleepNode() {
        this(null);
    }

    public SleepNode(NodeConfig nodeConfig) {
        super(nodeConfig);
        this.sleetTime = DEFAULT_SLEEP_TIME;
    }

    @Override
    public void run(GraphContext context) {
        try {
            Thread.sleep(sleetTime);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
