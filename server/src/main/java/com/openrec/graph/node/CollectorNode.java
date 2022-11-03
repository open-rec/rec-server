package com.openrec.graph.node;

import com.openrec.graph.GraphContext;
import com.openrec.graph.tools.anno.Import;
import com.openrec.proto.model.ScoreResult;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public class CollectorNode extends SyncNode<Void>{

    @Import("operationItems")
    private List<ScoreResult> finalItems;

    @Override
    public void run(GraphContext context) {
        context.setResult(finalItems);
        log.info("{} return with final item size:{}", getName(), finalItems.size());
    }
}
