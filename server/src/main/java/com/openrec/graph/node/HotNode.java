package com.openrec.graph.node;

import static com.openrec.graph.RecParams.SCENE;

import java.util.List;

import org.assertj.core.util.Lists;

import com.openrec.graph.GraphContext;
import com.openrec.graph.config.HotConfig;
import com.openrec.graph.config.NodeConfig;
import com.openrec.graph.tools.anno.Export;
import com.openrec.proto.model.ScoreResult;
import com.openrec.service.recall.RecallStore;
import com.openrec.util.BeanUtil;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class HotNode extends RecallNode<HotConfig> {
    private RecallStore recallStore = BeanUtil.getBean(RecallStore.class);
    @Export("hotItems")
    private List<ScoreResult> hotItems;

    public HotNode(NodeConfig nodeConfig) {
        super(nodeConfig);
        this.hotItems = Lists.newArrayList();
    }

    @Override
    public void run(GraphContext context) {

        String scene = context.getParams().getValueToString(SCENE);
        boolean open = config.isOpen();

        if (!open) {
            log.info("{} not open, just return", getName());
            return;
        }

        int size = config.getContent().getSize();

        hotItems = recallStore.hot(tableName(), scene, size);
        exportChannel(context, hotItems);
        log.info("{} type:{} table:{} with hot item size:{}",
            getName(), recallType(), tableName(), hotItems.size());
    }
}
