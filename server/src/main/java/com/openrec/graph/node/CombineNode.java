package com.openrec.graph.node;

import com.openrec.graph.GraphContext;
import com.openrec.graph.config.CombineConfig;
import com.openrec.graph.config.NodeConfig;
import com.openrec.graph.tools.anno.Export;
import com.openrec.graph.tools.anno.Import;
import com.openrec.proto.model.ScoreResult;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.util.Lists;

import java.util.List;

@Slf4j
public class CombineNode extends SyncNode<CombineConfig> {
    @Import("i2iItems")
    private List<ScoreResult> i2iItems;

    @Import("newItems")
    private List<ScoreResult> newItems;

    @Import("hotItems")
    private List<ScoreResult> hotItems;

    @Export("combineItems")
    private List<ScoreResult> combineItems;

    public CombineNode(NodeConfig nodeConfig) {
        super(nodeConfig);
    }


    @Override
    public void run(GraphContext context) {

        combineItems = Lists.newArrayList();
        combineItems.addAll(i2iItems);
        combineItems.addAll(hotItems);
        combineItems.addAll(newItems);

        int size = config.getContent().getSize();
        combineItems = combineItems.subList(0, Math.min(combineItems.size(), size));
        log.info("{} with result size:{}", getName(), combineItems.size());
    }
}
