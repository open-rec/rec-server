package com.openrec.graph.node;

import com.openrec.graph.GraphContext;
import com.openrec.graph.config.CombineConfig;
import com.openrec.graph.tools.anno.Export;
import com.openrec.graph.tools.anno.Import;
import com.openrec.proto.model.ScoreResult;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.util.Lists;

import java.util.List;

@Slf4j
public class CombineNode extends SyncNode<CombineConfig>{
    @Import("i2iItems")
    private List<ScoreResult> i2iItems;

    @Import("newItems")
    private List<ScoreResult> newItems;

    @Import("hotItems")
    private List<ScoreResult> hotItems;

    @Export("recallItems")
    private List<ScoreResult> recallItems;

    @Override
    public void run(GraphContext context) {

        recallItems = Lists.newArrayList();
        recallItems.addAll(i2iItems);
        recallItems.addAll(hotItems);
        recallItems.addAll(newItems);

        int size = config.getContent().getSize();
        recallItems = recallItems.subList(0, size);
        log.info("{} with result size:{}" ,getName(), recallItems.size());
    }
}
