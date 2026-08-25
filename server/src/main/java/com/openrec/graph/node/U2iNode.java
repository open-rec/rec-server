package com.openrec.graph.node;

import static com.openrec.graph.RecParams.SCENE;
import static com.openrec.graph.RecParams.USER_ID;

import java.util.List;

import org.assertj.core.util.Lists;

import com.openrec.graph.GraphContext;
import com.openrec.graph.config.NodeConfig;
import com.openrec.graph.config.U2iConfig;
import com.openrec.graph.tools.anno.Export;
import com.openrec.proto.model.ScoreResult;
import com.openrec.service.recall.RecallStore;
import com.openrec.util.BeanUtil;

import lombok.extern.slf4j.Slf4j;

/** Looks up a precomputed user-to-item recall table by scene and request user. */
@Slf4j
public class U2iNode extends RecallNode<U2iConfig> {

    private RecallStore recallStore = BeanUtil.getBean(RecallStore.class);

    @Export("u2iItems")
    private List<ScoreResult> u2iItems;

    public U2iNode(NodeConfig nodeConfig) {
        super(nodeConfig);
        this.u2iItems = Lists.newArrayList();
    }

    @Override
    public void run(GraphContext context) {
        if (!config.isOpen()) {
            log.info("{} not open, just return", getName());
            return;
        }
        String scene = context.getParams().getValueToString(SCENE);
        String userId = context.getParams().getValueToString(USER_ID);
        u2iItems = recallStore.u2i(tableName(), scene, userId, config.getContent().getSize());
        exportChannel(context, u2iItems);
        log.info("{} type:{} table:{} with u2i size:{}",
            getName(), recallType(), tableName(), u2iItems.size());
    }
}
